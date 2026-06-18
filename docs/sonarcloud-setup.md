# SonarCloud — inrichting en onderhoud

Documentatie van de **eenmalige externe inrichting** (SonarCloud + GitHub Secrets) en **troubleshooting** wanneer CI faalt. De CI-config staat in de repo; drempels en NFR-koppeling staan in `[01-nfr-onderhoudbaarheid.md](01-nfr-onderhoudbaarheid.md)`; branch protection en OTAP-context in `[otap.md](otap.md)`.

**Dashboard:** [sonarcloud.io/project/overview?id=bami-flogert_openmrs-module-htmlformentry](https://sonarcloud.io/project/overview?id=bami-flogert_openmrs-module-htmlformentry)

---

## Repo-bestanden


| Bestand                                                   | Rol                                                                       |
| --------------------------------------------------------- | ------------------------------------------------------------------------- |
| `[sonar-project.properties](../sonar-project.properties)` | Org `bami-flogert`, project key, exclusions (`.github/**`); JaCoCo via standaardpad per module |
| `[.github/workflows/ci.yml](../.github/workflows/ci.yml)` | Job `sonarcloud`: tests + `sonar:sonar` met `sonar.qualitygate.wait=true` |


Build en tests draaien op **JDK 8**; de Sonar-scanner op **JDK 17** (scanner-vereiste).

---

## Verificatie (setup afgerond)

- [x] Organization en project key komen overeen met `sonar-project.properties`
- [x] `SONAR_TOKEN` in [GitHub Actions secrets](https://github.com/bami-flogert/openmrs-module-htmlformentry/settings/secrets/actions)
- [x] GitHub-integratie + PR decoration actief in SonarCloud
- [x] **Automatic Analysis uit**, **CI-based Analysis aan** (anders: *"CI analysis while Automatic Analysis is enabled"*)
- [x] Eerste PR-run groen (Quality Gate Passed)
- [x] Branch protection: required check **SonarCloud Analysis** op `acceptatie` en `main` — zie [`otap.md` § Branch protection](otap.md#checklist-branch-protection)

**Let op:** de GitHub App-check *"SonarCloud Code Analysis"* is niet hetzelfde als de workflow-job **SonarCloud Analysis** in `ci.yml`.

Quality Gate-drempels (Sonar way, free plan): zie NFR-M1 t/m M7 in `[01-nfr-onderhoudbaarheid.md](01-nfr-onderhoudbaarheid.md)`.

---

## Eenmalige setup (referentie)

Alleen nodig bij nieuw teamlid, verlopen token of nieuw SonarCloud-project:

1. SonarCloud → **My Account → Security** → token `github-actions-htmlformentry`
2. GitHub → **Secrets → Actions** → `SONAR_TOKEN` (exact)
3. SonarCloud → **Analysis Method**: automatic uit, CI-based aan

---

## Troubleshooting: Quality Gate FAILED

Als de scan draait maar Maven eindigt met *QUALITY GATE STATUS: FAILED*, werkt `sonar.qualitygate.wait=true` zoals bedoeld (NFR-M5/M6).

1. Open de Sonar-link uit de CI-log (bijv. `…&pullRequest=<nr>`)
2. **Quality Gate** → welke conditie is rood? (vaak: *Coverage on New Code*, *New Issues*)
3. Issues oplossen in code, of false positives als *Won't fix* markeren
4. Re-run de **SonarCloud Analysis** job op de PR

**Veelvoorkomend:** gewijzigde `.github/workflows/ci.yml` telt als new code — daarom `sonar.exclusions=.github/**` in `sonar-project.properties`.

### JaCoCo-import in Sonar-scan

Sonar leest per Maven-module standaard `target/site/jacoco/jacoco.xml` (gegenereerd door `jacoco:report` in de `verify`-fase). Geen custom `sonar.coverage.jacoco.xmlReportPaths` nodig.

De `sonarcloud`-job in `ci.yml` bevat een stap **Verify JaCoCo reports exist** die controleert op `api` en `omod` vóór `sonar:sonar`. (`api-tests` heeft geen `src/main` — daar wordt geen `jacoco.xml` gegenereerd; integratietests dekken code in `api`/`omod`.)

**Eerdere fout (opgelost):** repo-root-paden (`omod/target/site/jacoco/jacoco.xml` in parent `pom.xml`) werden per module verkeerd opgelost — Sonar zocht `omod/omod/target/...` en logde *No report imported*. Fix: property verwijderd; standaardpad per module.

Als import alsnog faalt: controleer CI-log op `OK: …/jacoco.xml` en `Reading report` in de Sonar-stap. Fallback: **unit-test**-artifact (`jacoco-report-pr-<nr>`) of lokaal `omod/target/site/jacoco/`. Zie [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md).

---

