<div align="center">

# Auditrapport

## Security & Compliance audit — OpenMRS HTML Form Entry

<br>

**Module:** OpenMRS HTML Form Entry (`openmrs-module-htmlformentry`) v3.10.0
**Normkader:** NEN-7510:2024-2 · AVG · EU Cyber Resilience Act (CRA)
**Scope:** Controls A.8.3 · A.8.5 · A.8.15 + CI/CD-pipeline

<br>

---

## Inhoudsopgave

1. [Executive Summary](#1-executive-summary)
2. [Scope & Context](#2-scope--context)
3. [Audit Methodologie](#3-audit-methodologie)
4. [Risico-analyse & Bevindingen](#4-risico-analyse--bevindingen)
5. [SBOM & Supply Chain Security](#5-sbom--supply-chain-security)
6. [Conclusie & Advies](#6-conclusie--advies)
7. [Bijlagen](#7-bijlagen)

---

## 1. Executive Summary

Deze audit toetst de OpenMRS-module **HTML Form Entry v3.10.0** — die bijzondere
persoonsgegevens in de zin van AVG art. 9 verwerkt — aan **NEN-7510:2024-2**
(controls A.8.3, A.8.5, A.8.15), aangevuld met de CI/CD-pipeline. De aanpak
combineert handmatige code-review, statische analyse (Snyk SAST/SCA, zizmor),
SBOM-generatie en een penetratietest met live exploitatie in een Docker-lab.

**Eindoordeel:** de module is **functioneel toegangsbeveiligd via de OpenMRS-kern**,
maar kent op modulecode-niveau bevindingen die in deze audit zijn aangetoond én
gemitigeerd. Alle drie de NEN-controls scoren **⚠️ Gedeeltelijk** in de
[gap-analyse](01-gap-analyse.md); de drie meest kritische, zelf-exploiteerbare
kwetsbaarheden zijn met een pentest aangetoond en daarna in de modulecode hersteld
en hertest (zie [§4](#4-risico-analyse--bevindingen)).

**Drie belangrijkste risico's (vóór mitigatie):**

1. **Datalek via arbitrary file read** (HFE-01, CWE-22/200) — de
   bestandspreview-controller las elk opgegeven pad in; aangetoond credential-lek.
   Raakt het hoogste risico uit de [risicomatrix](04-risicomatrix.md) (D2 datalek, score 20).
2. **Datamanipulatie zonder autorisatie/CSRF** (HFE-02, CWE-862/352/639) — encounters
   konden cross-site en zonder privilege ge-void worden (risico D3, integriteit medicatie).
3. **Verouderde, kwetsbare afhankelijkheden** (o.a. log4j 1.x, commons-fileupload,
   Spring4Shell, gebundelde jQuery) — zie [security backlog](06-security-backlog.md)
   en [patchadvies](07-patchadvies.md).

**Status na mitigatie:** HFE-01, HFE-02 en HFE-03 zijn opgelost in de modulecode en
met een hertest reproduceerbaar aangetoond als gesloten (zie de `-na`-bevindingen in
[§4](#4-risico-analyse--bevindingen)). De afhankelijkheids-risico's zijn geprioriteerd
in een patchplan met een verwachte risicoreductie van 100% bij volledige uitvoering.

---

## 2. Scope & Context

**Geauditeerd object.** OpenMRS HTML Form Entry (`openmrs-module-htmlformentry`)
**v3.10.0** — een module waarmee zorgverleners klinische formulieren invoeren en
verwerken. De keuze voor deze module en de motivatie staan in
[`module-keuze.md`](../module-keuze.md).

**Waarom relevant.** De module verwerkt bijzondere persoonsgegevens en valt daarmee onder AVG art. 9 en
de zorgspecifieke beveiligingsnorm NEN-7510. De gegevensbeschermingscontext is apart
uitgewerkt in de [DPIA-check](08-dpia-check.md). De kroonjuwelen en hun
BIV-classificatie staan in [`03-assets.md`](03-assets.md).

**Normkader.**

- **NEN-7510:2024-2** — primair getoetst op de controls **A.8.3**,
  **A.8.5** en **A.8.15**; zie [gap-analyse](01-gap-analyse.md).
- **AVG** — bijzondere persoonsgegevens; zie [DPIA-check](08-dpia-check.md).
- **EU Cyber Resilience Act (CRA)** — supply-chain/SBOM-context en essentiële eisen; zie [§5](#5-sbom--supply-chain-security) en de [CRA-mapping](11-cra-mapping.md) (bijlage H).

**Scope-grens.**

- **In scope:** de modulecode,
  de afhankelijkheden van de module en de CI/CD-pipeline
  ([pipeline-compliance](02-pipeline-compliance.md), [`otap.md`](../otap.md)).
- **Buiten scope:** OpenMRS Core/platform zelf. Platform-afhankelijkheden (`org.openmrs.api:*`,
  `org.openmrs.web:*`, scope `provided`) zijn wél geïnventariseerd maar niet door de module
  patchbaar — zie [patchadvies §5](07-patchadvies.md).

---

## 3. Audit Methodologie

De audit is opgebouwd uit vijf elkaar aanvullende sporen:

| # | Stap | Aanpak / tooling | Vastlegging |
|---|------|------------------|-------------|
| 1 | **Gap-analyse NEN-7510** | Handmatige code-review, elke subeis gemapt op NEN-control met bewijs op `bestand:regel` | [`01-gap-analyse.md`](01-gap-analyse.md) |
| 2 | **Risicoweging** | Dreigingen afgeleid van de kroonjuwelen; `risico = kans × impact` (1–5), aangevuld met bow-tie-threatmodellen | [`04-risicomatrix.md`](04-risicomatrix.md), [`05-bowtie.md`](05-bowtie.md) |
| 3 | **Dependency- & SAST-scan** | Snyk SCA (`snyk test --all-projects`) + Snyk Code, zizmor voor GitHub Actions; contextuele her-scoring van CVSS naar bereikbaarheid | [`06-security-backlog.md`](06-security-backlog.md) |
| 4 | **SBOM & patchadvies** | SPDX (`deploy.yml`) + CycloneDX (`snyk sbom`); geprioriteerd patchplan via [`generate-patch-advice.py`](../../.github/scripts/generate-patch-advice.py) | [`07-patchadvies.md`](07-patchadvies.md) |
| 5 | **Penetratietest** | Reproduceerbare exploitatie in een Docker-lab met een gepatchte module-build (`Dockerfile.pentest`); vóór- en ná-bevindingen met bewijs | [`../pentest/`](../pentest/) |

De **contextuele score** corrigeert de algemene CVSS-score voor de
daadwerkelijke bereikbaarheid van het aanvalspad in déze module en het geraakte
kroonjuweel uit [`03-assets.md`](03-assets.md). Een critical CVE in ongebruikte code
weegt zo lichter, een medium CVE in het formulier-invoerpad juist zwaarder. De
pipeline-/OTAP-context en de scheiding van omgevingen staan in
[`02-pipeline-compliance.md`](02-pipeline-compliance.md) en [`../otap.md`](../otap.md).

---

## 4. Risico-analyse & Bevindingen

### 4.1 Compliance-gap per NEN-control

Samenvatting van de [gap-analyse](01-gap-analyse.md) (bijlage A):

| Control | Oordeel | Kritieke hiaten |
|---------|---------|-----------------|
| **A.8.3** Toegangsbeveiliging | ⚠️ Gedeeltelijk | Ontbrekende `@Authorized` op de service-laag; geen gedocumenteerde privilege-matrix |
| **A.8.5** Veilige authenticatie | ⚠️ Gedeeltelijk | Geen CSRF-bescherming; sessiebeheer niet modulespecifiek geconfigureerd |
| **A.8.15** Logging | ⚠️ Gedeeltelijk | PII-logging gemitigeerd (HFE-04 — alleen `patientId`/`userId`/IDs); resterend: geen retentie-/rotatiebeleid; geen log-integriteit |

### 4.2 Risicobeeld

De [risicomatrix](04-risicomatrix.md) (bijlage F) identificeert twee rode risico's —
**D2 datalek via phishing/credential-diefstal (score 20)** en **D1 ransomware
(score 15)** — uitgewerkt als bow-tie-threatmodellen in [`05-bowtie.md`](05-bowtie.md).

### 4.3 Aangetoonde kwetsbaarheden

Drie zelf-exploiteerbare kwetsbaarheden zijn in het lab aangetoond én daarna in de
modulecode gemitigeerd en hertest. Per bevinding is er een vóór- en een ná-document
met bewijs in [`../pentest/`](../pentest/):

| ID | Kwetsbaarheid (CWE) | Component | Bow-tie | Vóór | Ná (gemitigeerd) |
|----|---------------------|-----------|---------|------|------------------|
| **HFE-01** | Arbitrary file read / path traversal (CWE-22/73/200) | `HtmlFormFromFileController` | D2 datalek (C=5) | [voor](../pentest/bevinding-hfe-01-voor.md) | [na ✅](../pentest/bevinding-hfe-01-na.md) |
| **HFE-02** | Missing authz + CSRF + IDOR (CWE-862/352/639) | `DeleteEncounterController` | datamanipulatie (I=5) | [voor](../pentest/bevinding-hfe-02-voor.md) | [na ✅](../pentest/bevinding-hfe-02-na.md) |
| **HFE-03** | Open redirect (CWE-601) | `DeleteEncounterController` | D2 datalek/phishing | [voor](../pentest/bevinding-hfe-03-voor.md) | [na ✅](../pentest/bevinding-hfe-03-na.md) |

Het bewijsmateriaal (HTTP-responses, JSON, screenshots) staat in
[`../pentest/bewijs/`](../pentest/bewijs/). De `-na`-documenten bevatten per bevinding
een voor/ná-tabel die aantoont dat het aanvalspad gesloten is terwijl de legitieme
functionaliteit blijft werken.

Naast deze drie is een vierde, privacy-gerelateerde bevinding vastgelegd en hertest:
**HFE-04** — PII (naam/geboortedatum) in applicatielogs (A.8.15, AVG art. 9). Deze is
gemitigeerd door alleen numerieke `patientId`/`userId`/IDs te loggen en is met een
hertest bevestigd ([voor](../pentest/bevinding-hfe-04-voor.md) · [na ✅](../pentest/bevinding-hfe-04-na.md)).

### 4.4 Kwetsbare afhankelijkheden

De [security backlog](06-security-backlog.md) (bijlage I) registreert de
SCA-bevindingen, contextueel gescoord en gemapt op NEN-controls. Het register is
vastgesteld (2026-06-11); open H-prioriteiten worden daar bijgehouden tot mitigatie
of geaccepteerde uitzondering. Pentest-bevindingen (HFE-01 t/m HFE-04) staan
apart in [`../pentest/`](../pentest/) — zie [ID-legenda](../pentest/README.md#twee-id-systemen--niet-door-elkaar-halen).
De H-prioriteiten — o.a. **HFE-001** (log4j 1.x, EOL), **HFE-002** (commons-fileupload,
RCE via deserialisatie), **HFE-003** (gebundelde jQuery, DOM-XSS) en **HFE-004**
(Spring4Shell) — vormen de input voor het [patchadvies](07-patchadvies.md) (bijlage J).

---

## 5. SBOM & supply chain-beveiliging

Overzicht van afhankelijkheden, verouderde/kwetsbare componenten en maatregelen ter bescherming van de keten. Detail per component: [`07-patchadvies.md`](07-patchadvies.md); vollediger register: [`06-security-backlog.md`](06-security-backlog.md).

**Huidige pipeline-maatregelen:**

- SPDX SBOM via [`deploy.yml`](../../.github/workflows/deploy.yml) job `sbom` (GitHub dependency graph, gebundeld in `otap-build-bundle`)
- CycloneDX SBOM via [`snyk.yml`](../../.github/workflows/snyk.yml) (`snyk sbom`)
- Dependabot voor Maven, GitHub Actions en Docker ([`dependabot.yml`](../../.github/dependabot.yml))
- Dependency review op pull requests ([`ci.yml`](../../.github/workflows/ci.yml))
- GitHub Actions workflow-SAST via [zizmor](https://zizmor.sh/) — lokaal `zizmor .` ([`zizmor.md`](../zizmor.md))
- Geprioriteerd patchadvies op basis van SBOM + CVE/CVSS ([`07-patchadvies.md`](07-patchadvies.md), gegenereerd via [`generate-patch-advice.py`](../../.github/scripts/generate-patch-advice.py))

De toetsing van deze pipeline-/supply-chainmaatregelen aan de essentiële CRA-eisen (Annex I, Deel I en II) staat in de [CRA-mapping](11-cra-mapping.md) (bijlage H).

Zie ook bijlage B ([`02-pipeline-compliance.md`](02-pipeline-compliance.md)) en het OTAP-overzicht ([`../otap.md`](../otap.md)).

---

## 6. Conclusie & Advies

**Eindoordeel.** De module steunt voor toegangsbeveiliging sterk op de OpenMRS-kern
en scoort daardoor op de hoofdfunctionaliteit redelijk, maar op modulecode-niveau
zijn reële bevindingen aangetoond. De drie controls A.8.3, A.8.5 en A.8.15 zijn
**gedeeltelijk** compliant (zie [gap-analyse](01-gap-analyse.md)). De drie meest
kritische, zelf-exploiteerbare kwetsbaarheden (HFE-01/02/03) zijn met een pentest
aangetoond en vervolgens **opgelost en hertest** ([§4.3](#43-aangetoonde-kwetsbaarheden-penetratietest)).
Het grootste resterende risico zit in **verouderde afhankelijkheden** die deels via
het platform binnenkomen.

**Geprioriteerd advies.**

| Prioriteit | Aanbeveling | Verwijzing |
|-----------|-------------|------------|
| **P1 — direct** | Patch de critical Maven-/Docker-componenten in de geadviseerde volgorde (Jackson, MySQL 5.6→8.0, commons-fileupload). Verwachte risicoreductie 100% bij volledige uitvoering. | [patchadvies §2–4](07-patchadvies.md) |
| **P1 — direct** | Borg de reeds doorgevoerde code-mitigaties (HFE-01/02/03) in een release en regressietest. | [pentest `-na`](../pentest/) |
| **P2 — inplannen** | Upgrade de gebundelde front-end-libraries (jQuery/jQuery UI/handlebars, HFE-003/007) en zet de groovy-scope op `test` (HFE-006) — beide binnen de module oplosbaar. | [backlog §6.5](06-security-backlog.md) |
| **P2 — inplannen** | Dicht de gap-hiaten: `@Authorized` op de service-laag (A.8.3), CSRF-bescherming modulebreed (A.8.5), PII-pseudonimisering + logretentie (A.8.15). | [gap-analyse](01-gap-analyse.md) |
| **P3 — borgen** | Houd de OTAP-scheiding en SBOM/Dependabot-pipeline operationeel; verifieer niet-herleidbare data per omgeving. | [pipeline-compliance](02-pipeline-compliance.md), [DPIA](08-dpia-check.md) |

De volledige verbeteraanpak met prioritering staat in de
[security backlog](06-security-backlog.md) (bijlage I) en het
[patchadvies](07-patchadvies.md) (bijlage J).

---

## 7. Bijlagen

| # | Bijlage | Bestand | Status |
|---|---------|---------|--------|
| A | Gap-analyse | [`01-gap-analyse.md`](01-gap-analyse.md) | Aanwezig |
| B | Mini-complianceverslag pipeline | [`02-pipeline-compliance.md`](02-pipeline-compliance.md) | Aanwezig (bijgewerkt 2026-06-09) |
| — | OTAP-pipeline (operationeel) | [`../otap.md`](../otap.md) | Aanwezig |
| — | GitHub Actions SAST (zizmor) | [`../zizmor.md`](../zizmor.md) | Aanwezig (baseline 2026-06-15) |
| C | SBOM | `deploy.yml` — `build-sbom` / `otap-build-bundle` (SPDX); `snyk.yml` — `snyk-sbom.json` (CycloneDX) | ✅ Aanwezig — SPDX gekoppeld aan build-run; CycloneDX via Snyk |
| D | SAST-output (Snyk) | `snyk.yml` artifacts `snyk-results.json`, `snyk-code-results.json` | ⚠️ Gedeeltelijk — vereist `SNYK_TOKEN`; `continue-on-error: true` |
| E | Traceability matrix | [`10-traceability-matrix.md`](10-traceability-matrix.md) | Aanwezig (2026-06-17) |
| F | Risicomatrix | [`04-risicomatrix.md`](04-risicomatrix.md) (`risicomatrix.png`) | Aanwezig |
| G | Bow-tie / threat model | [`05-bowtie.md`](05-bowtie.md) (`bowtie-*.png`) | Aanwezig |
| H | CRA-mapping | [`11-cra-mapping.md`](11-cra-mapping.md) | Aanwezig (2026-06-18) |
| I | Security backlog / verbeteraanpak | [`06-security-backlog.md`](06-security-backlog.md) | Aanwezig (2026-06-11) — register vastgesteld; H-prioriteiten deels open |
| J | Patchadvies (SBOM/CVE) | [`07-patchadvies.md`](07-patchadvies.md) | Aanwezig — gegenereerd via Snyk + [`generate-patch-advice.py`](../../.github/scripts/generate-patch-advice.py) |
| K | DPIA-check (AVG art. 9/35) | [`08-dpia-check.md`](08-dpia-check.md) | Aanwezig (2026-06-15) |
| L | Verantwoording (AI-)tooling | [`09-verantwoording-tooling.md`](09-verantwoording-tooling.md) | Aanwezig (2026-06-18) — afgerond |


