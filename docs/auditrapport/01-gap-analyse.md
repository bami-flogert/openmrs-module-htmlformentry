# Gap-analyse — NEN-7510:2024-2

**Module:** OpenMRS HTML Form Entry v3.10.0  
**Datum:** 2026-06-03  
**Beoordelaar:** smickel20  
**Scope:** Controls A.8.3 · A.8.5 · A.8.15

---

## Legenda

| Status | Betekenis |
|--------|-----------|
| ✅ Aanwezig | Control is aantoonbaar geïmplementeerd |
| ⚠️ Gedeeltelijk | Control is deels aanwezig, maar bevat hiaten |
| ❌ Afwezig | Control ontbreekt of is niet aantoonbaar |

---

## A.8.3 — Toegangsbeveiliging (Access Restriction)

> **Norm:** Toegang tot informatie en applicatiefuncties moet beperkt worden op basis van het vastgestelde toegangsbeheerbeleid.

### Eindbeoordeling: ⚠️ Gedeeltelijk

### Bevindingen

| # | Subeis | Status | Bewijs |
|---|--------|--------|--------|
| 1 | Privilege-check bij beheersfuncties | ✅ Aanwezig | `HtmlFormFromFileController.java:48` — `Context.requirePrivilege("Manage Forms")` blokkeert niet-geautoriseerde gebruikers actief |
| 2 | Privilege-constante op admin-extensiepunt | ✅ Aanwezig | `AdminList.java:71` — retourneert `PrivilegeConstants.MANAGE_FORMS` als vereist recht voor de adminlijst |
| 3 | Privilege-check voor formulierzichtbaarheid | ✅ Aanwezig | `FormEntryHandlerExtension.java:76` — `hasPrivilege(PrivilegeConstants.VIEW_UNPUBLISHED_FORMS)` |
| 4 | UI-niveau toegangscontrole op invoer | ✅ Aanwezig | `htmlFormEntry.jsp:284` — `<openmrs:hasPrivilege privilege="Edit Encounters,Edit Observations">` |
| 5 | UI-niveau toegangscontrole op verwijderen | ✅ Aanwezig | `htmlFormEntry.jsp:296` — `<openmrs:hasPrivilege privilege="Delete Encounters,Delete Observations">` |
| 6 | UI-niveau toegangscontrole op patiëntformulieren | ✅ Aanwezig | `patientHtmlForms.jsp:3` — `<openmrs:hasPrivilege privilege="Form Entry">` |
| 7 | Rolgebaseerde toegangscontrole op API-laag | ⚠️ Gedeeltelijk | Service-methoden in `HtmlFormEntryServiceImpl` bevatten geen `@Authorized`-annotaties; handhaving verloopt uitsluitend via de OpenMRS-kerninfrastructuur |
| 8 | Beveiliging van REST/DWR-eindpunten | ⚠️ Gedeeltelijk | `config.xml:98-114` — DWR-methoden zijn geconfigureerd, maar er zijn geen expliciete privilege-guards op `DWRHtmlFormEntryService` zichtbaar buiten de sessiecontrole |
| 9 | Documentatie toegangsbeheerbeleid | ⚠️ Gedeeltelijk | [`docs/security.md`](../security.md) — kwetsbaarheidsmelding + privilege-matrix; OTAP-toegang in [`otap.md`](../otap.md) |

### Gebreken

- **Gebrek:** `@Authorized`-annotaties ontbreken op serviceklassen — bij een misconfiguratie van de beveiligingscontext zou de API-laag toegankelijk kunnen zijn zonder privilege-check.  
  **Verbetering:** Voeg `@Authorized({PrivilegeConstants.MANAGE_FORMS})` toe aan schrijfmethoden in `HtmlFormEntryServiceImpl`.

- **Gebrek:** Geen mapping van rollen naar privileges — de module documenteert wel vereiste privileges per functie, maar niet welke standaardrollen die krijgen (instellingsbeleid).  
  **Verbetering:** Privilege-matrix staat in [`security.md`](../security.md); rol-toewijzing documenteren in implementatiehandleiding van de zorginstelling.

---

## A.8.5 — Veilige Authenticatie (Secure Authentication)

> **Norm:** Veilige authenticatieprocedures en -technologieën moeten worden geïmplementeerd op basis van beperkingen voor informatietoegang.

### Eindbeoordeling: ⚠️ Gedeeltelijk

### Bevindingen

| # | Subeis | Status | Bewijs |
|---|--------|--------|--------|
| 1 | Controleren of gebruiker is ingelogd | ✅ Aanwezig | `DWRHtmlFormEntryService.java:17` — `Context.isAuthenticated()` als bewakingscheck |
| 2 | Authenticatie via platform-context | ✅ Aanwezig | `DWRHtmlFormEntryService.java:29` — `Context.authenticate(user, pass)` delegeert naar OpenMRS-kernbeveiliging |
| 3 | Ingelogde gebruiker ophalen bij gegevensverwerking | ✅ Aanwezig | `HtmlFormEntryController.java:237,258` — `Context.getAuthenticatedUser()` bij formulierverwerking |
| 4 | Ingelogde gebruiker ophalen bij formuliergeneratie | ✅ Aanwezig | `HtmlFormEntryGenerator.java:814,834` — gebruikerscontext voor providertoewijzing |
| 5 | CSRF-bescherming | ⚠️ Gedeeltelijk | Geen synchronizer-tokens modulebreed; wel same-origin check op `DeleteEncounterController` (`isSameOrigin`) na pentest HFE-02 — zie [`bevinding-hfe-02-na.md`](../pentest/bevinding-hfe-02-na.md) |
| 6 | Sessietime-out / automatische vergrendeling | ❌ Afwezig | Niet geconfigureerd in de module; volledig afhankelijk van de OpenMRS-container |
| 7 | Multi-factor authenticatie | ❌ Afwezig | Niet aanwezig — valt buiten de moduleScope, maar er is ook geen koppeling met MFA-extensies |
| 8 | Wachtwoordbeleid | ❌ Afwezig | Niet in scope voor deze module; geen verwijzing naar platform-wachtwoordbeleid |
| 9 | Foutmelding bij mislukte login | ⚠️ Gedeeltelijk | Authenticatie via DWR — niet gecontroleerd of foutmelding onderscheid maakt tussen gebruiker/wachtwoord |

### Gebreken

- **Gebrek (gedeeltelijk gemitigeerd):** Geen module-brede CSRF-bescherming op formulierverzendingen — kwetsbaar voor cross-site request forgery bij patiëntgegevens. Op `deleteEncounter` blokkeert een same-origin check (`Origin`/`Referer`) tokenloze cross-site POSTs (pentest HFE-02). Dat is geen volwaardig synchronizer-token op alle state-changing endpoints.  
  **Verbetering:** Breid CSRF-bescherming uit: synchronizer-token patroon of Spring Security CSRF in `HtmlFormEntryController` en overige POST-endpoints.

- **Gebrek:** Sessiebeheer volledig gedelegeerd aan de container zonder modulespecifieke instellingen.  
  **Verbetering:** Documenteer de verwachte sessietime-out en verwijs naar de OpenMRS Runtime Properties (`web.xml`).

---

## A.8.15 — Logging

> **Norm:** Logboeken die activiteiten, uitzonderingen, fouten en andere relevante gebeurtenissen registreren, moeten worden aangemaakt, opgeslagen, beschermd en geanalyseerd.

### Eindbeoordeling: ⚠️ Gedeeltelijk

### Bevindingen

| # | Subeis | Status | Bewijs                                                                                                                                     |
|---|--------|--------|--------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | Logging framework geconfigureerd | ✅ Aanwezig | `api/src/main/resources/log4j.xml:6-16` — Log4j met CONSOLE-appender, ISO8601-tijdstempel, methodepatroon                                  |
| 2 | Logger aanwezig in kernklassen | ✅ Aanwezig | `FormEntrySession.java:79`, `FormSubmissionController.java:31`, `HtmlFormEntryServiceImpl.java:44`, `HtmlFormEntryController.java:54`      |
| 3 | Toegangslogging bij patiëntgegevens | ✅ Aanwezig | `FormEntryAuditLogFormatter.java` + `FormEntrySession.java` — metadata-only INFO bij sessie-start (`action=session.created`) en succesvolle submit (`action=submit.success`); zie [`08-logging.md`](../08-logging.md) |
| 4 | Foutlogging bij formulierverwerking | ✅ Aanwezig | `HtmlFormEntryController.java:285,323,329` — `log.error` bij validatie-, invoer- en verzendfouten                                          |
| 5 | Audittrail via creator/changedBy op data | ✅ Aanwezig | `HtmlFormEntryServiceImpl.java:120,124` — `setCreator` / `setChangedBy` met ingelogde gebruiker                                            |
| 6 | Audittrail bij void-acties | ✅ Aanwezig | `FormEntrySession` — DEBUG via `FormEntryAuditLogFormatter.formatVoidObsDebugMessage()`; `FormSubmissionActions` — VOID/CHANGED via `formatObsReference()` |
| 7 | Wijzigingsregistratie | ✅ Aanwezig | `FormSubmissionActions` + `FormEntryAuditLogFormatter` — DEBUG alleen `obsId`/`conceptId`; standaard uitgeschakeld door INFO-default in `log4j.xml` |
| 8 | Creator-tracking op orders en programma-inschrijvingen | ✅ Aanwezig | `FormSubmissionActions.java:511,533`, `DrugOrderSubmissionElement.java:622,653`                                                            |
| 9 | Logbescherming | ❌ Afwezig | Logbestanden worden naar console/file geschreven zonder integriteitsbeveiliging                                                            |
| 10 | Gecentraliseerde log-aggregatie | ❌ Afwezig | Geen koppeling met externe logging-systemen                                                                                                |
| 11 | Retentiebeleid voor logbestanden | ❌ Afwezig | Log4j-configuratie stelt geen rotatie of retentietermijn in                                                                                |
| 12 | Beveiligingsgebeurtenissen apart gelogd | ⚠️ Gedeeltelijk | Beveiligingsgebeurtenissen worden niet apart gecategoriseerd — alles gaat naar dezelfde logger                                             |
| 13 | Logging van PII conform AVG/NEN-7510 | ⚠️ Gedeeltelijk | INFO-paden PoC-fix (geen namen/dob/gender); rest-risico: `HtmlFormEntryUtil.java:327` logt volledige XML op ERROR |

### Gebreken

- **Gebrek (opgelost in PoC):** PII (patiëntnaam, geboortedatum, geslacht) werd in plaintext gelogd op INFO.  
  **Mitigatie:** metadata-only logging in [`08-logging.md`](../08-logging.md) — `session.created` en `submit.success`. Pentest: [`bevinding-hfe-04-voor.md`](../pentest/bevinding-hfe-04-voor.md) / [`bevinding-hfe-04-na.md`](../pentest/bevinding-hfe-04-na.md).

- **Gebrek:** Geen logretentie- of rotatiebeleid geconfigureerd — logbestanden kunnen onbeperkt groeien of worden overschreven.  
  **Verbetering:** Voeg Log4j `RollingFileAppender` toe met retentie van minimaal 1 jaar (NEN-7510 vereiste voor medische logs).

- **Gebrek:** Geen integriteitsbeveiliging van logbestanden — logs kunnen worden gewijzigd zonder detectie.  
  **Verbetering:** Route logs naar een centrale, beveiligde log-aggregator.

---

## Samenvatting

| Control | Status | Kritieke hiaten |
|---------|--------|-----------------|
| A.8.3 Toegangsbeveiliging | ⚠️ Gedeeltelijk | Ontbrekende `@Authorized` op service-laag; geen rol→privilege-mapping (privilege-matrix wel gedocumenteerd) |
| A.8.5 Authenticatie | ⚠️ Gedeeltelijk | CSRF alleen op `deleteEncounter` (same-origin); geen tokens modulebreed; sessiebeheer niet modulespecifiek geconfigureerd |
| A.8.15 Logging | ⚠️ Gedeeltelijk | Geen retentiebeleid; geen log-integriteitsbeveiliging; geen dedicated security logger; rest-risico XML in ERROR-log |
