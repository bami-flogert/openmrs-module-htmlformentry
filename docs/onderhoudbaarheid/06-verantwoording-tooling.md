# Verantwoording tooling — onderhoudbaarheid

**Datum:** 17 juni 2026 · **Branch:** `acceptatie`


| Fase                          | Document / tooling                                                  | Status |
| ----------------------------- | ------------------------------------------------------------------- | ------ |
| Characterization tests (AI)   | `[05-verantwoording-ai-tests.md](../05-verantwoording-ai-tests.md)` | ✅      |
| PoC-refactor (Extract Method) | `HtmlFormEntryController.getFormEntrySession`                       | ✅      |
| SonarCloud CI / quality gate  | `[sonarcloud-setup.md](../sonarcloud-setup.md)`                     | ✅      |
| Test- en CI-bewijs            | `[04-testresultaten-baseline.md](../04-testresultaten-baseline.md)` | ✅      |


Volledige voor/na-beoordeling (NFR-M1 t/m M4): `[07-validatie-voor-na.md](../07-validatie-voor-na.md)` (Teamlid B).

---

## PoC-refactor — tooling en aanpak

**Gepland:** Extract Class → `FormEntryRequestResolver`.  
**Uitgevoerd:** Extract Method + Move Method binnen `HtmlFormEntryController` en `FormEntrySession`.


| Hulpmiddel             | Rol                                                                                                        |
| ---------------------- | ---------------------------------------------------------------------------------------------------------- |
| IDE-refactoring        | Extract Method (`resolveMode`, `resolveFormEntryContext`, `buildFormEntrySession`, …)                      |
| Characterization tests | Regressievangnet vóór en na refactor (`[05-verantwoording-ai-tests.md](../05-verantwoording-ai-tests.md)`) |
| SonarCloud             | Validatie CC (S3776) en new-code smells                                                                    |
| JaCoCo                 | Coverage-bewijs op PoC-hotspot                                                                             |


**Resultaat:** `getFormEntrySession` CC **52 → geen S3776-violation** op changed code. Coverage `HtmlFormEntryController` **2,1% → ~63%** (JaCoCo).

---

## SonarCloud en CI

### Inrichting

Zie `[sonarcloud-setup.md](../sonarcloud-setup.md)`: `SONAR_TOKEN`, CI-based analysis, branch protection met verplichte check **SonarCloud Analysis**.

### `sonar.qualitygate.wait=true`

In `[.github/workflows/ci.yml](../../.github/workflows/ci.yml)` wacht de `sonarcloud`-job op de Quality Gate vóór de job als geslaagd telt. Bij **FAILED** faalt de pipeline — bewuste merge-rem (NFR-M5/M6).

**Bewijs:** PR #31 — [CI-run 27682116331](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27682116331), log: `QUALITY GATE STATUS: PASSED`.

### Metrieken na refactor (PR #31)


| Metriek                | Waarde  |
| ---------------------- | ------- |
| New S3776 (CC)         | 0       |
| New code smells        | 5 Major |
| Maintainability rating | A       |
| Quality Gate           | Passed  |


### JaCoCo-import in Sonar-scan (opgelost)

**Oorzaak:** `sonar.coverage.jacoco.xmlReportPaths` in parent `pom.xml` gebruikte repo-root-paden (`omod/target/...`). Sonar resolveert die per module vanuit de module-directory, waardoor het rapport niet gevonden werd (*No report imported*).

**Fix (optie 1):** property verwijderd uit `pom.xml` en `sonar-project.properties`. Sonar gebruikt nu het standaardpad `target/site/jacoco/jacoco.xml` per module (`api`, `omod`). CI-stap **Verify JaCoCo reports exist** in `ci.yml` bevestigt aanwezigheid vóór `sonar:sonar` (`api-tests` heeft geen productiebroncode).

**Verificatie (PR #37):** [CI-run 27749957935](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27749957935) — JaCoCo verify `OK` voor `api` + `omod`; Sonar-log: `QUALITY GATE STATUS: PASSED`; dashboard [PR #37](https://sonarcloud.io/dashboard?id=bami-flogert_openmrs-module-htmlformentry&pullRequest=37). Modules zonder `src/main` (`api-tests`) en versie-api's zonder eigen jacoco.xml loggen nog *No report imported* — verwacht.

### Nieuwe smells — trade-off

Vijf Major smells op changed code, geen blockers:

1. **FormEntrySession** (4×): geneste `if`s en `RuntimeException` in `validateNotModifiedSinceTimestamps` — logisch gevolg van Move Method uit de controller.
2. **HtmlFormEntryController** (1×): `buildFormEntrySession` met 9 parameters — gevolg van Extract Method-orchestratie.

Geen gedragsregressie: 67 omod-tests + regressie-subset 58/58 groen. Opruimen is optioneel (parameter-object, specifieke exceptions, merge `if`s); niet nodig voor Quality Gate.

---

## Kritische reflectie


| Aspect                       | Beoordeling                                                                                                                                                                          |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **SonarCloud**               | Nuttig voor CC-detectie (S3776) en merge-gate; JaCoCo-import via standaardpad per module (fix juni 2026) |
| **AI (tests)**               | Characterization tests maakten Extract Method veilig; zie `[05-verantwoording-ai-tests.md](../05-verantwoording-ai-tests.md)`                                                        |
| **Ontwerp vs implementatie** | Extract Method i.p.v. Extract Class — sneller, minder bestanden; 5 new smells en controller nog ~567 LOC zijn acceptabele trade-offs (B documenteert in `05-ontwerp-refactoring.md`) |


