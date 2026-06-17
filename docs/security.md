# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 3.10.x  | :white_check_mark: |
| 3.9.x   | :white_check_mark: |
| < 3.9   | :x:                |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Report vulnerabilities to the OpenMRS security team by emailing **security@openmrs.org**.
Include as much detail as possible:

- A description of the vulnerability and its potential impact
- Steps to reproduce or a proof-of-concept
- The affected version(s)
- Any suggested mitigations, if known

### What to expect

- **Acknowledgement** within 3 business days confirming receipt of your report
- **Status update** within 10 business days with an assessment and expected timeline
- Credit in the release notes if the vulnerability is confirmed and fixed (unless you prefer to remain anonymous)

If a vulnerability is **accepted**, we will work on a fix and coordinate a disclosure date with you before publishing a CVE or public advisory.

If a vulnerability is **declined** (e.g. not reproducible, out of scope, or intended behavior), we will explain our reasoning.

We ask that you give us reasonable time to address the issue before any public disclosure.

---

## Module security documentation

Deze module verwerkt klinische gegevens (AVG art. 9). Onderstaande documenten vullen deze disclosure policy aan met audit-, pentest- en pipeline-bewijs.

| Document | Inhoud |
|----------|--------|
| [`auditrapport/00-auditrapport.md`](auditrapport/00-auditrapport.md) | Hoofdrapport security & compliance (NEN-7510) |
| [`auditrapport/01-gap-analyse.md`](auditrapport/01-gap-analyse.md) | Gap-analyse controls A.8.3, A.8.5, A.8.15 |
| [`auditrapport/06-security-backlog.md`](auditrapport/06-security-backlog.md) | CVE/dependency-backlog (HFE-001, …) |
| [`auditrapport/07-patchadvies.md`](auditrapport/07-patchadvies.md) | Geprioriteerde patches (subset van Snyk-scan) |
| [`pentest/README.md`](pentest/README.md) | Pentest-overzicht (HFE-01 t/m HFE-04) |
| [`08-logging.md`](08-logging.md) | Logging- en privacybeleid (NFR-S1/S2) |
| [`zizmor.md`](zizmor.md) | GitHub Actions security scan |
| [`otap.md`](otap.md) | OTAP-pipeline en deploy-toegang |

**ID-nummering:** pentest (`HFE-01`) en CVE-backlog (`HFE-001`) gebruiken dezelfde module-prefix maar verschillende reeksen — zie [`pentest/README.md#twee-id-systemen--niet-door-elkaar-halen`](pentest/README.md#twee-id-systemen--niet-door-elkaar-halen).

---

## Toegangsbeheer (privileges)

OpenMRS koppelt **rollen** aan **privileges**; de module handhaaft rechten via privileges op UI-, controller- en (deels) platformniveau. Welke rol welk privilege heeft, configureert de zorginstelling in OpenMRS — niet in deze module.

| Privilege | Waar afgedwongen | Functie |
|-----------|------------------|---------|
| **Form Entry** | `patientHtmlForms.jsp` | Patiëntformulieren openen en invullen |
| **Edit Encounters**, **Edit Observations** | `htmlFormEntry.jsp` | Formulier bewerken / obs wijzigen |
| **Delete Encounters**, **Delete Observations** | `htmlFormEntry.jsp` (UI), `DeleteEncounterController` (server) | Encounter voiden |
| **Manage Forms** | `HtmlFormFromFileController`, `htmlFormFromFile.jsp`, `htmlForm.jsp`, `htmlFormList.jsp`, `htmlFormSchema.jsp`, admin-extensie | Formulierbeheer, bestandspreview |
| **View Unpublished Forms** | `FormEntryHandlerExtension` | Ongepubliceerde formulieren tonen |
| **View Forms** | `htmlForms.jsp`, migratie-JSP's | Formulierlijst en migraties |
| **View Patients** | `personSearch.jsp` | Patiëntzoeken in widgets |

**Niet in deze matrix:** DWR-RPC (`DWRHtmlFormEntryService`) en de meeste `@RequestMapping`-controllers vertrouwen op OpenMRS-sessie en module-default filters — zie [`pentest/00-attack-surface-mapping.md`](pentest/00-attack-surface-mapping.md). Service-laag (`HtmlFormEntryServiceImpl`) heeft geen `@Authorized`-annotaties; zie gap-analyse A.8.3.

Pipeline- en omgevingstoegang (OTAP): [`otap.md`](otap.md) en [`auditrapport/02-pipeline-compliance.md`](auditrapport/02-pipeline-compliance.md).

