# Module-keuze: OpenMRS HTML Form Entry

## Gekozen module

| Eigenschap | Waarde |
|-----------|--------|
| **Naam** | OpenMRS HTML Form Entry (`openmrs-module-htmlformentry`) |
| **Versie** | 3.10.0 |
| **Taal** | Java (Maven-module, Spring MVC) |
| **Broncode** | https://github.com/openmrs/openmrs-module-htmlformentry/tree/3.10.0 |

---

## Motivatie

### 1. Kritieke functionaliteit

De module is verantwoordelijk voor het weergeven, invullen en opslaan van alle forms binnen  OpenMRS. Elke interactie raakt direct aan persoonsgegevens en medische gegevens van de AVG. Een beveiligingsfout in deze module heeft daardoor onmiddellijk impact.

### 2. Breedte van de scope

De module raakt de drie relevante regelgebieden van de NEN-7510:

| NEN-7510 gebied | Aanwezigheid in module |
|----------------|----------------------|
| **Toegangsbeveiliging** (11.x) | Privilege-checks op form-acties en submit-handlers |
| **Authenticatie & autorisatie** | Gebruik van OpenMRS `Context.getAuthenticatedUser()` en role-checks |
| **Audit logging** | Vastleggen van form-submits, wijzigingen en fouten |
| **Invoervalidatie / data-integriteit** | Verwerking van vrije HTML/SQL-invoer via formuliervelden |


### 3. Complexiteit en omvang

De module telt meerdere tientallen Java-klassen, een eigen tag-library, een Velocity/Freemarker templatelaag en integratiepunten met de OpenMRS Core API. De complexiteit is groot genoeg voor een zinvolle analyse.

---

## Gerelateerde documentatie

| Document | Inhoud |
|----------|--------|
| [`auditrapport/00-auditrapport.md`](auditrapport/00-auditrapport.md) | Hoofdrapport security & compliance audit |
| [`auditrapport/01-gap-analyse.md`](auditrapport/01-gap-analyse.md) | Gap-analyse NEN-7510 controls A.8.3 / A.8.5 / A.8.15 |
| [`auditrapport/02-pipeline-compliance.md`](auditrapport/02-pipeline-compliance.md) | Pipeline-compliance (bijlage B) |
| [`otap.md`](otap.md) | OTAP-pipeline: branches, omgevingen, workflows |
| [`onderhoudbaarheid/01-nfr-onderhoudbaarheid.md`](onderhoudbaarheid/01-nfr-onderhoudbaarheid.md) | NFR onderhoudbaarheid (JaCoCo, SonarCloud) |
| [`security.md`](security.md) | Vulnerability disclosure policy |

