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
| 9 | Documentatie toegangsbeheerbeleid | ⚠️ Gedeeltelijk | [`docs/security.md`](../security.md) — vulnerability disclosure; geen privilege-matrix of OTAP-toegangsbeleid (zie [`otap.md`](../otap.md)) |

### Gebreken

- **Gebrek:** `@Authorized`-annotaties ontbreken op serviceklassen — bij een misconfiguratie van de beveiligingscontext zou de API-laag toegankelijk kunnen zijn zonder privilege-check.  
  **Verbetering:** Voeg `@Authorized({PrivilegeConstants.MANAGE_FORMS})` toe aan schrijfmethoden in `HtmlFormEntryServiceImpl`.

- **Gebrek:** Geen gedocumenteerd overzicht van welke rollen welke privileges vereisen.  
  **Verbetering:** Maak een privilege-matrix op in `docs/` of in `config.xml` als commentaar. OTAP- en pipeline-toegang is wel gedocumenteerd in [`otap.md`](../otap.md) en [`02-pipeline-compliance.md`](02-pipeline-compliance.md).

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
| 5 | CSRF-bescherming | ❌ Afwezig | Geen CSRF-tokens aangetroffen in formulieren, filters of controllers |
| 6 | Sessietime-out / automatische vergrendeling | ❌ Afwezig | Niet geconfigureerd in de module; volledig afhankelijk van de OpenMRS-container |
| 7 | Multi-factor authenticatie | ❌ Afwezig | Niet aanwezig — valt buiten de moduleScope, maar er is ook geen koppeling met MFA-extensies |
| 8 | Wachtwoordbeleid | ❌ Afwezig | Niet in scope voor deze module; geen verwijzing naar platform-wachtwoordbeleid |
| 9 | Foutmelding bij mislukte login | ⚠️ Gedeeltelijk | Authenticatie via DWR — niet gecontroleerd of foutmelding onderscheid maakt tussen gebruiker/wachtwoord |

### Gebreken

- **Gebrek:** Geen CSRF-bescherming op formulierverzendingen — kwetsbaar voor cross-site request forgery bij formulierinvoer van patiëntgegevens.  
  **Verbetering:** Implementeer synchronizer-token patroon of gebruik Spring Security CSRF-ondersteuning in `HtmlFormEntryController`.

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
| 3 | Toegangslogging bij patiëntgegevens | ✅ Aanwezig | `FormEntrySession.java:145-148` — logt patiënt-ID, geboortedatum, geslacht en naam bij elke formuliersessie                                |
| 4 | Foutlogging bij formulierverwerking | ✅ Aanwezig | `HtmlFormEntryController.java:285,323,329` — `log.error` bij validatie-, invoer- en verzendfouten                                          |
| 5 | Audittrail via creator/changedBy op data | ✅ Aanwezig | `HtmlFormEntryServiceImpl.java:120,124` — `setCreator` / `setChangedBy` met ingelogde gebruiker                                            |
| 6 | Audittrail bij void-acties | ✅ Aanwezig | `FormEntrySession.java:660` — `log.debug("voiding obs: " + o.getObsId())` en `FormSubmissionActions.java:320`                              |
| 7 | Wijzigingsregistratie | ✅ Aanwezig | `FormSubmissionActions.java:335` — logt `oldString + " -> " + newString` per concept                                                       |
| 8 | Creator-tracking op orders en programma-inschrijvingen | ✅ Aanwezig | `FormSubmissionActions.java:511,533`, `DrugOrderSubmissionElement.java:622,653`                                                            |
| 9 | Logbescherming | ❌ Afwezig | Logbestanden worden naar console/file geschreven zonder integriteitsbeveiliging                                                            |
| 10 | Gecentraliseerde log-aggregatie | ❌ Afwezig | Geen koppeling met externe logging-systemen                                                                                                |
| 11 | Retentiebeleid voor logbestanden | ❌ Afwezig | Log4j-configuratie stelt geen rotatie of retentietermijn in                                                                                |
| 12 | Beveiligingsgebeurtenissen apart gelogd | ⚠️ Gedeeltelijk | Beveiligingsgebeurtenissen worden niet apart gecategoriseerd — alles gaat naar dezelfde logger                                             |
| 13 | Logging van PII conform AVG/NEN-7510 | ⚠️ Gedeeltelijk | `FormEntrySession.java:145-148` logt patiëntnaam, geboortedatum en geslacht in plaintext — dit is AVG die beschermd opgeslagen moet worden |

### Gebreken

- **Gebrek:** PII (patiëntnaam, geboortedatum, geslacht) wordt in plaintext gelogd.  
  **Verbetering:** PoC-fix in [`08-logging.md`](../08-logging.md) — metadata-only logging (`patientId`, `userId`, `action`). Pentest: [`bevinding-hfe-04-voor.md`](../pentest/bevinding-hfe-04-voor.md) / [`bevinding-hfe-04-na.md`](../pentest/bevinding-hfe-04-na.md).

- **Gebrek:** Geen logretentie- of rotatiebeleid geconfigureerd — logbestanden kunnen onbeperkt groeien of worden overschreven.  
  **Verbetering:** Voeg Log4j `RollingFileAppender` toe met retentie van minimaal 1 jaar (NEN-7510 vereiste voor medische logs).

- **Gebrek:** Geen integriteitsbeveiliging van logbestanden — logs kunnen worden gewijzigd zonder detectie.  
  **Verbetering:** Route logs naar een centrale, beveiligde log-aggregator.

---

## Samenvatting

| Control | Status | Kritieke hiaten |
|---------|--------|-----------------|
| A.8.3 Toegangsbeveiliging | ⚠️ Gedeeltelijk | Ontbrekende `@Authorized` op service-laag; geen privilege-matrix |
| A.8.5 Authenticatie | ⚠️ Gedeeltelijk | Geen CSRF-bescherming; sessiebeheer niet modulespecifiek geconfigureerd |
| A.8.15 Logging | ⚠️ Gedeeltelijk | PII in plaintext in logs; geen retentiebeleid; geen log-integriteitsbeveiliging |
