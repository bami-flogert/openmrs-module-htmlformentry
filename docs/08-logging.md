# Logging — OpenMRS HTML Form Entry

**Project:** OpenMRS HTML Form Entry v3.10.0  
**Datum:** 15 juni 2026  
**Normkader:** NEN-7510 A.8.15 · NFR-S1 · NFR-S2

Baseline en gap-analyse: [`auditrapport/01-gap-analyse.md`](auditrapport/01-gap-analyse.md) §A.8.15.  
Pipeline-audittrail: [`auditrapport/02-pipeline-compliance.md`](auditrapport/02-pipeline-compliance.md).

---

## Doel

Applicatielogging in de module voldoet aan **0 PII in logs** (NFR-S1) en **metadata-only audit logging** (NFR-S2). Dit document beschrijft wat we wijzigen in de PoC en welke regels ontwikkelaars volgen.

---

## Wat we doen (PoC)

| # | Actie | Bestand |
|---|-------|---------|
| 1 | PII INFO-log vervangen door `patientId`, `userId`, `action` | `FormEntrySession.java` → `FormEntryAuditLogFormatter.formatSessionCreated()` |
| 2 | Module-default logniveau DEBUG → INFO | `log4j.xml` (api, omod, api-tests) |
| 3 | DEBUG in form-submit: geen klinische waarden, alleen `obsId`/`conceptId` | `FormSubmissionActions.printObsHelper()` → `FormEntryAuditLogFormatter.formatObsReference()` |
| 4 | Pentest + grep-validatie documenteren | [`pentest/bevinding-hfe-04-voor.md`](pentest/bevinding-hfe-04-voor.md) · [`pentest/bevinding-hfe-04-na.md`](pentest/bevinding-hfe-04-na.md) |
| 5 | Submit INFO-log: `patientId`, `userId`, `htmlFormId`, `encounterId`, `mode`, `action=submit.success` | `FormEntrySession.applyActions()` → `FormEntryAuditLogFormatter.formatSubmitSuccess()` |
| 6 | Log injection voorkomen (Sonar java:S5145): alleen numerieke IDs via formatter | `FormEntryAuditLogFormatter.java` |
| 7 | Geautomatiseerde regressie audit-logging | `FormEntryAuditLogFormatterTest`, `FormEntrySessionLoggingTest`, `FormEntrySessionTest`, `PostSubmissionActionTagTest` — zie [`03-teststrategie.md`](03-teststrategie.md) §7.6 |

---

## Ontwerpkeuze: applicatielog vs database-audit

| Laag | Rol | Waarom |
|------|-----|--------|
| **Applicatielog (INFO)** | Wie / wanneer / welk formulier / welke patiënt (ID) bij openen en succesvol opslaan | Snelle operationele audit; pentest-vriendelijk; A.8.15 activiteiten |
| **Database (`creator`/`changedBy`)** | Welke entiteiten gewijzigd zijn per obs, encounter, order | Authoritatief voor klinische verantwoording; al aanwezig in OpenMRS |
| **Niet gelogd** | Klinische waarden, namen, request/XML-body | AVG dataminimalisatie (NFR-S1) |

**Waarom submit-logging is toegevoegd:** openen van een formulier (`session.created`) bewijst geen succesvolle schrijfactie. Persist van klinische data is de kritieke activiteit onder A.8.15.

**Waarom geen obs-waarden in logs:** redundant met de database; hoog privacyrisico. DEBUG-paden zijn gesaniteerd en standaard uitgeschakeld (INFO-default).

**Waarom een centrale formatter:** één plek voor metadata-only berichten; voorkomt string-concatenatie van domain-objecten (log injection / Sonar java:S5145).

---

## Event inventory

| Event | Logged? | Sensitive data | NEN A.8.15? |
|-------|---------|----------------|-------------|
| Formuliersessie geopend | ✅ INFO — `FormEntrySession` constructor | Metadata only (`patientId`, `userId`, `action=session.created`) | ✅ Activiteit (toegang) |
| Formulier succesvol opgeslagen | ✅ INFO — `FormEntrySession.applyActions()` | Metadata only (`patientId`, `userId`, `htmlFormId`, `encounterId`, `mode`, `action=submit.success`) | ✅ Activiteit (data write) |
| Validatie-/submit-fout | ✅ ERROR — `HtmlFormEntryController` | Stacktrace; geen patient in logmessage | ✅ Uitzonderingen |
| Obs void/change | ✅ DEBUG (uit bij INFO-default) | Alleen `obsId`/`conceptId` via `FormEntryAuditLogFormatter` | ✅ Wijziging (indirect) |
| XML parse failure | ✅ ERROR — `HtmlFormEntryUtil:327` | **Volledige XML-body** — bekend rest-risico | ❌ Schendt NFR-S1-beleid |
| Module start/stop | ✅ INFO — `HtmlFormEntryActivator` | Geen | ✅ Operationeel |
| Pipeline build/deploy | ✅ GitHub Actions artifacts | Geen patiëntdata | ✅ Pipeline-laag (bijlage B) |
| Auth failure / privilege denial | ❌ | — | ❌ Gap (geen security-logger) |
| Log retentie / integriteit | ❌ | — | ❌ Gap (geen RollingFileAppender) |

---

## Regels voor ontwikkelaars

Gebruik Apache Commons Logging (`LogFactory.getLog(getClass())`) — geen nieuwe logging-frameworks in deze module.

| Regel | Toelichting |
|-------|-------------|
| Geen PII op INFO/WARN/ERROR | Geen namen, geboortedatum, geslacht, BSN, vrije medische tekst |
| Metadata-first | `patientId`, `userId`, `encounterId`, `htmlFormId`, `mode`, `action`, `durationMs` |
| Centrale formatter | Nieuwe audit- of DEBUG-logs via `FormEntryAuditLogFormatter`; geen directe string-concatenatie van domain-objecten |
| Fouten zonder payload | `log.error("Exception during form validation", ex)` — stacktrace ja, request-body nee |
| Audit op data | OpenMRS `setCreator` / `setChangedBy` blijft de primaire wijzigingsregistratie |
| Geen `getPatientIdentifier()` in logs | Gebruik intern `patientId`; identifier kan een zichtbaar patiëntnummer zijn |

**Toegestaan:** `patientId=42`, `userId=7`, `action=session.created`, `action=submit.success`, `htmlFormId=3`, `encounterId=101`, `mode=ENTER`, `durationMs=230`  
**Verboden:** `names=`, `dob=`, `gender=`, `obs.getValueAsString()`, volledige request/XML-body

Null-waarden in audit logs: `none` voor IDs; null `mode` → `unknown`.

---

## Logniveaus

| Omgeving | Default level `org.openmrs.module.htmlformentry` |
|----------|--------------------------------------------------|
| Ontwikkeling / test / acceptatie | INFO (module `log4j.xml`; bevestig runtime-config op server) |
| Productie (advies) | WARN — configureer via server `log4j` indien nodig |

DEBUG alleen tijdelijk lokaal voor troubleshooting; nooit met klinische waarden loggen.

---

## Validatie

1. Open HTML-formulier voor testpatiënt (geauthenticeerd).
2. Dien formulier succesvol in.
3. Inspecteer applicatielog (`catalina.out` of console).
4. Verwacht na openen: `FormEntrySession created: patientId=... userId=... action=session.created`
5. Verwacht na submit: `Form submission completed: ... action=submit.success`
6. Geautomatiseerde PII-check:

```bash
grep -E "names=|dob=|gender=|PersonName" catalina.out && echo FAIL || echo PASS
```

7. Geautomatiseerde regressie: zie [`03-teststrategie.md`](03-teststrategie.md) §7.6 (unit + integratie).

Pentest-bewijs: [HFE-04 vóór](pentest/bevinding-hfe-04-voor.md) · [HFE-04 na](pentest/bevinding-hfe-04-na.md).

---

## Buiten scope (PoC)

- Log4j 1.x → 2.x migratie (backlog HFE-001)
- `RollingFileAppender` + retentiebeleid
- Centrale log-aggregatie / integriteitsbewaking
- Aparte security-logger categorie
- `HtmlFormEntryUtil` XML-body in ERROR-log (vervolgactie)

---

## Referenties

| Document | Relatie |
|----------|---------|
| [`opdracht/non-functionals.md`](../opdracht/non-functionals.md) | NFR-S1, NFR-S2, NFR-S6 |
| [`auditrapport/01-gap-analyse.md`](auditrapport/01-gap-analyse.md) | Baseline A.8.15 |
| [`03-teststrategie.md`](03-teststrategie.md) | §7.6 logging audit tests |
| [`opdracht/voortgang-en-todo.md`](../opdracht/voortgang-en-todo.md) | Pentest-status |
