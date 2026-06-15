# Patchadvies — SBOM & CVE/CVSS

**Module:** OpenMRS HTML Form Entry v3.10.0  
**Datum:** 2026-06-15  
**Status:** Gegenereerd uit Snyk-scan — representatief; ververs bij audit uit CI-artifact snyk-results  
**Bronnen:** SPDX SBOM (`deploy.yml`), CycloneDX SBOM (`snyk sbom`), Snyk SCA (Maven + Docker)

---

## 1. Samenvatting

- **Totaal kwetsbaarheden:** 7 (6 unieke componenten)
- **Critical:** 2 | **High:** 4 | **Medium:** 1 | **Low:** 0
- **Baseline risk score:** 47.2 (som max-CVSS per component, cap 10)

## 2. Geprioriteerde patchvolgorde (Maven)

| # | Component | Huidig | Advies | CVE(s) | CVSS | Reden prioriteit |
|---|-----------|--------|--------|--------|------|------------------|
| 1 | `org.codehaus.jackson:jackson-mapper-asl` | 1.5.0 | 2.15.0 | CVE-2019-10202 | 9.8 | P1: max CVSS 9.8 (critical); upgrade-pad beschikbaar |
| 2 | `org.codehaus.jackson:jackson-core-asl` | 1.5.0 | 2.15.0 | CVE-2019-10199 | 8.1 | P2: max CVSS 8.1 (high); upgrade-pad beschikbaar |
| 3 | `commons-fileupload:commons-fileupload` | 1.2 | 1.5 | CVE-2016-1000033 | 7.5 | P3: max CVSS 7.5 (high); upgrade-pad beschikbaar |
| 4 | `commons-codec:commons-codec` | 1.4 | 1.13 | CVE-2012-5783 | 5.3 | P4: max CVSS 5.3 (medium); upgrade-pad beschikbaar |

## 3. Docker / infrastructuur

Images uit [`docker-compose.yml`](../../docker-compose.yml):

| # | Image | Huidig | Advies | CVE(s) | CVSS | Reden prioriteit |
|---|-------|--------|--------|--------|------|------------------|
| 1 | `mysql:5.6.51` | 5.6.51 | 8.0.36 | CVE-2020-14765, CVE-2021-3711 | 9.1 | P1: max CVSS 9.1 (critical); infrastructuur/EOL-risico; 2 CVE's; upgrade-pad beschikbaar |
| 2 | `openmrs/openmrs-reference-application-distro` | 2.12.2 | 2.13.0 | CVE-2022-3602 | 7.4 | P2: max CVSS 7.4 (high); infrastructuur/EOL-risico; upgrade-pad beschikbaar |

## 4. Verwachte risicoreductie

Model: per component wordt de hoogste CVSS-score (max 10) opgeteld als baseline. Bij elke patch wordt die score afgetrokken; cumulatief percentage = `(baseline − resterend) / baseline`.

> Schatting op basis van Snyk upgrade paths. Regressietests (`mvn test`, smoke test) zijn vereist na elke upgrade.

### Gecombineerde volgorde

| Stap | Actie | CVSS-oplossing | Cumulatieve reductie | Ernst |
|------|-------|----------------|----------------------|-------|
| 1 | org.codehaus.jackson:jackson-mapper-asl → 2.15.0 | 9.8 | 20.8% | critical |
| 2 | mysql:5.6.51 → 8.0.36 | 9.1 | 40.0% | critical |
| 3 | org.codehaus.jackson:jackson-core-asl → 2.15.0 | 8.1 | 57.2% | high |
| 4 | commons-fileupload:commons-fileupload → 1.5 | 7.5 | 73.1% | high |
| 5 | openmrs/openmrs-reference-application-distro → 2.13.0 | 7.4 | 88.8% | high |
| 6 | commons-codec:commons-codec → 1.13 | 5.3 | 100.0% | medium |

**Verwachte totale risicoreductie na alle stappen:** 100.0% (baseline 47.2 → resterend 0.0)

### Alleen Maven

| Stap | Actie | CVSS-oplossing | Cumulatieve reductie |
|------|-------|----------------|----------------------|
| 1 | org.codehaus.jackson:jackson-mapper-asl → 2.15.0 | 9.8 | 31.9% |
| 2 | org.codehaus.jackson:jackson-core-asl → 2.15.0 | 8.1 | 58.3% |
| 3 | commons-fileupload:commons-fileupload → 1.5 | 7.5 | 82.7% |
| 4 | commons-codec:commons-codec → 1.13 | 5.3 | 100.0% |

## 5. Niet-patchbare items (module-scope)

| Component | Huidige situatie | Reden |
|-----------|------------------|-------|
| `org.openmrs.api:*` | 1.9.9 (default profiel) | OpenMRS-platformversie (provided scope); beheerd via Maven-profiel, genegeerd door Dependabot |
| `org.openmrs.web:*` | 1.9.9 (default profiel) | OpenMRS-platformversie (provided scope); upgrade vereist platformmigratie |

## 6. Inputbestanden

| Bron | Bestand | Aanwezig |
|------|---------|----------|
| Maven SCA | `snyk-results.json` | ✅ |
| Docker SCA | `snyk-container-mysql.json` | ✅ |
| Docker SCA | `snyk-container-openmrs.json` | ✅ |

## 7. Verwijzingen

- SPDX SBOM: [`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml) → `docs/sbom.spdx.json`
- CycloneDX SBOM: [`.github/workflows/snyk.yml`](../../.github/workflows/snyk.yml) → `snyk-sbom.json`
- OTAP-pipeline: [`docs/otap.md`](../otap.md)
- Hoofdrapport: [`00-auditrapport.md`](00-auditrapport.md)

---

_Gegenereerd door [`.github/scripts/generate-patch-advice.py`](../../.github/scripts/generate-patch-advice.py)._
