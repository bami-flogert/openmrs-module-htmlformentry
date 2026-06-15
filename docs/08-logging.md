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
| 1 | PII INFO-log vervangen door `patientId`, `userId`, `action` | `FormEntrySession.java` |
| 2 | Module-default logniveau DEBUG → INFO | `log4j.xml` (api, omod, api-tests) |
| 3 | DEBUG in form-submit: geen klinische waarden, alleen `obsId`/`conceptId` | `FormSubmissionActions.java` |
| 4 | Pentest + grep-validatie documenteren | [`pentest/bevinding-hfe-04-voor.md`](pentest/bevinding-hfe-04-voor.md) · [`pentest/bevinding-hfe-04-na.md`](pentest/bevinding-hfe-04-na.md) |

---

## Regels voor ontwikkelaars

Gebruik Apache Commons Logging (`LogFactory.getLog(getClass())`) — geen nieuwe logging-frameworks in deze module.

| Regel | Toelichting |
|-------|-------------|
| Geen PII op INFO/WARN/ERROR | Geen namen, geboortedatum, geslacht, BSN, vrije medische tekst |
| Metadata-first | `patientId`, `userId`, `encounterId`, `formId`, `action`, `durationMs` |
| Fouten zonder payload | `log.error("Exception during form validation", ex)` — stacktrace ja, request-body nee |
| Audit op data | OpenMRS `setCreator` / `setChangedBy` blijft de primaire wijzigingsregistratie |
| Geen `getPatientIdentifier()` in logs | Gebruik intern `patientId`; identifier kan een zichtbaar patiëntnummer zijn |

**Toegestaan:** `patientId=42`, `userId=7`, `action=session.created`, `durationMs=230`  
**Verboden:** `names=`, `dob=`, `gender=`, `obs.getValueAsString()`, volledige request/XML-body

---

## Logniveaus

| Omgeving | Default level `org.openmrs.module.htmlformentry` |
|----------|--------------------------------------------------|
| Ontwikkeling / test / acceptatie | INFO |
| Productie (advies) | WARN — configureer via server `log4j` indien nodig |

DEBUG alleen tijdelijk lokaal voor troubleshooting; nooit met klinische waarden loggen.

---

## Validatie

1. Open HTML-formulier voor testpatiënt (geauthenticeerd).
2. Inspecteer applicatielog (`catalina.out` of console).
3. Verwacht: `FormEntrySession created: patientId=... userId=... action=session.created`
4. Geautomatiseerde check:

```bash
grep -E "names=|dob=|gender=|PersonName" catalina.out && echo FAIL || echo PASS
```

Pentest-bewijs: [HFE-04 vóór](pentest/bevinding-hfe-04-voor.md) · [HFE-04 na](pentest/bevinding-hfe-04-na.md).

---

## Buiten scope (PoC)

- Log4j 1.x → 2.x migratie (backlog HFE-001)
- `RollingFileAppender` + retentiebeleid
- Centrale log-aggregatie / integriteitsbewaking
- Aparte security-logger categorie

---

## Referenties

| Document | Relatie |
|----------|---------|
| [`opdracht/non-functionals.md`](../opdracht/non-functionals.md) | NFR-S1, NFR-S2, NFR-S6 |
| [`auditrapport/01-gap-analyse.md`](auditrapport/01-gap-analyse.md) | Baseline A.8.15 |
| [`opdracht/voortgang-en-todo.md`](../opdracht/voortgang-en-todo.md) | Pentest-status |
