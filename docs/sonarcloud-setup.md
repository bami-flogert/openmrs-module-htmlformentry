# SonarCloud — inrichting en onderhoud

Documentatie van de **eenmalige externe inrichting** (SonarCloud + GitHub Secrets) en **troubleshooting** wanneer CI faalt. De CI-config staat in de repo; drempels en NFR-koppeling staan in `[01-nfr-onderhoudbaarheid.md](01-nfr-onderhoudbaarheid.md)`; branch protection en OTAP-context in `[otap.md](otap.md)`.

**Dashboard:** [sonarcloud.io/project/overview?id=bami-flogert_openmrs-module-htmlformentry](https://sonarcloud.io/project/overview?id=bami-flogert_openmrs-module-htmlformentry)

---

## Repo-bestanden


| Bestand                                                   | Rol                                                                       |
| --------------------------------------------------------- | ------------------------------------------------------------------------- |
| `[sonar-project.properties](../sonar-project.properties)` | Org `bami-flogert`, project key, JaCoCo-paden, exclusions (`.github/`**)  |
| `[.github/workflows/ci.yml](../.github/workflows/ci.yml)` | Job `sonarcloud`: tests + `sonar:sonar` met `sonar.qualitygate.wait=true` |


Build en tests draaien op **JDK 8**; de Sonar-scanner op **JDK 17** (scanner-vereiste).

---

## Verificatie (setup afgerond)

- [x] Organization en project key komen overeen met `sonar-project.properties`
- [x] `SONAR_TOKEN` in [GitHub Actions secrets](https://github.com/bami-flogert/openmrs-module-htmlformentry/settings/secrets/actions)
- [x] GitHub-integratie + PR decoration actief in SonarCloud
- [x] **Automatic Analysis uit**, **CI-based Analysis aan** (anders: *"CI analysis while Automatic Analysis is enabled"*)
- [x] Eerste PR-run groen (Quality Gate Passed)
- [x] Branch protection: required check **SonarCloud Analysis** — zie `[otap.md` § Branch protection](otap.md#checklist-branch-protection)

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

### JaCoCo niet geïmporteerd in scan

Als de log meldt *No coverage report can be found* / *No report imported*: de `sonarcloud`-job draait `test verify` maar genereert geen `jacoco.xml` op de paden in `sonar.coverage.jacoco.xmlReportPaths` vóór `sonar:sonar`. Coverage in SonarCloud is dan leeg; gebruik het **unit-test**-artifact (`jacoco-report-pr-<nr>`) of lokaal `omod/target/site/jacoco/`. Zie [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md).

---

