# Testresultaten baseline — OpenMRS HTML Form Entry

**Datum:** 17 juni 2026 · **Branch:** `acceptatie` (PoC gemerged via PR #31)  
**Scope:** A = PoC (`omod`), B = module-baseline — zie [`03-teststrategie.md`](03-teststrategie.md)

---

## Uitvoering

| | |
|---|---|
| Omgeving | Windows 10, Temurin JDK 8, Maven 3.9.x |
| Scope A | `mvn -pl omod -am test verify` → **BUILD SUCCESS** (67 tests) |
| Scope B (subset) | 5 form-entry-klassen → **58 tests groen** (zie § Regressie-subset) |
| Scope B (volledig) | `mvn -pl api-tests test` → 529 tests, ~70 rood (gedocumenteerd, niet gefixt) |

---

## Resultaten Scope A (PoC / omod)

| Klasse | Tests | Status |
|--------|-------|--------|
| `HtmlFormEncounterControllerTest` | 7 | ✅ |
| `HtmlFormAjaxValidationControllerTest` | 2 | ✅ |
| `HtmlFormEntryControllerTest` | 13 | ✅ (T1–T9 characterization, ongewijzigd na refactor) |
| `HtmlFormEntryControllerExtractedMethodsTest` | 18 | ✅ (extract unit tests) |
| Overige omod-tests | 27 | ✅ |
| **Totaal omod** | **67** | **✅** |

Traceability: [`03-teststrategie.md`](03-teststrategie.md) §7.4 · §7.2b · AI-verantwoording: [`05-verantwoording-ai-tests.md`](05-verantwoording-ai-tests.md)

---

## Regressie-subset (api-tests, §7.5)

Vijf bestaande groene tests die het domein *form entry* raken — aangetoond na refactor-merge op `acceptatie`.

| Klasse | Tests | Motivatie |
|--------|-------|-----------|
| `ObsTagTest` | 37 | Obs/submit-flow in form entry |
| `HtmlFormTest` | 3 | HtmlForm-kern |
| `FormEntrySessionTest` | 4 | Session-lifecycle |
| `HtmlFormEntryServiceTest` | 6 | Service-laag submit |
| `FormEntrySessionValidateNotModifiedSinceTimestampsTest` | 8 | PoC Move Method (concurrency-check) |
| **Totaal subset** | **58** | **✅ BUILD SUCCESS** |

**Commando (reproduceerbaar):**

```powershell
mvn -B -pl api-tests "-Dtest=ObsTagTest,HtmlFormTest,FormEntrySessionTest,HtmlFormEntryServiceTest,FormEntrySessionValidateNotModifiedSinceTimestampsTest" test
```

---

## CI-bewijs

| Item | Waarde |
|------|--------|
| **CI-run (PR #31, groen)** | [actions/runs/27682116331](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27682116331) |
| **unit-test job** | [job/81871870059](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27682116331/job/81871870059) |
| **SonarCloud Analysis job** | [job/81871869907](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27682116331/job/81871869907) |
| **Quality Gate** | **PASSED** |
| **Sonar-dashboard (PR #31)** | [sonarcloud.io/…&pullRequest=31](https://sonarcloud.io/dashboard?id=bami-flogert_openmrs-module-htmlformentry&pullRequest=31) |
| **JaCoCo-artifact** | `jacoco-report-pr-31` (GitHub Actions artifact, 14 dagen bewaard) |

**JaCoCo-import fix (PR #37):**

| Item | Waarde |
|------|--------|
| **CI-run (PR #37, groen)** | [actions/runs/27749957935](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27749957935) |
| **SonarCloud Analysis job** | [job/82097648018](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27749957935/job/82097648018) |
| **JaCoCo verify-stap** | `OK: omod/.../jacoco.xml`, `OK: api/.../jacoco.xml` |
| **Quality Gate** | **PASSED** |
| **Sonar-dashboard (PR #37)** | [sonarcloud.io/…&pullRequest=37](https://sonarcloud.io/dashboard?id=bami-flogert_openmrs-module-htmlformentry&pullRequest=37) |

---

## SonarCloud — voor/na (PoC-scope)

Handoff-metrieken voor [`07-validatie-voor-na.md`](07-validatie-voor-na.md) (Teamlid B).

| Metriek | Voor (baseline 2026-06-11) | Na (PR #31 / `acceptatie`) | Bron |
|---------|---------------------------|----------------------------|------|
| `getFormEntrySession` CC | **52** | Geen S3776-issue (≤ 15) | SonarCloud new issues |
| OMOD S3776-violations | 5 | 0 **new** S3776 op changed code | SonarCloud |
| New code smells | — | **5 Major** | SonarCloud PR #31 |
| Maintainability rating | — | **A** | SonarCloud |
| Quality Gate | — | **Passed** | CI-log |
| `HtmlFormEntryController` coverage | 2,1% (3/143) | **~63%** (99/157 lines) | JaCoCo (lokaal + CI-artifact) |
| `FormEntryResolution` coverage | — | **100%** | JaCoCo |
| OMOD unit tests | 0 dedicated | **67 groen** | lokaal + CI |

**Nieuwe code smells (5 Major, PR #31):**

| # | Locatie | Beschrijving |
|---|---------|--------------|
| 1–2 | `FormEntrySession.java` L1049–1050 | Merge nested `if`; generic `RuntimeException` |
| 3–4 | `FormEntrySession.java` L1056–1058 | Idem (encounter-timestamp check) |
| 5 | `HtmlFormEntryController.java` L423 | `buildFormEntrySession` heeft 9 parameters (max 7) |

Gevolg van Move Method + Extract Method; geen gedragsregressie (58 regressie-subset-tests + 8 timestamp-tests groen). Zie [`06-verantwoording-tooling.md`](onderhoudbaarheid/06-verantwoording-tooling.md).

> **JaCoCo in Sonar:** fix geverifieerd op PR #37 — standaardpad per module; CI-log toont `OK` voor `api` en `omod` jacoco.xml + Quality Gate **PASSED**. Modules zonder productiecode (`api-tests`, `api-1.9`, …) loggen verwacht *No report imported*. PoC-coverage (`~63%` controller) blijft leidend via JaCoCo lokaal/artifact; controleer coverage % in Sonar-dashboard PR #37.

---

## JaCoCo (hotspot)

| Metriek | Baseline | Na PoC-tests + refactor |
|---------|----------|-------------------------|
| `HtmlFormEntryController` line coverage | 2,1% (3/143) | **~63%** (99/157 lines) |
| Instruction coverage | — | **56%** |
| Branch coverage | — | **53%** |
| Controller-pakket (instruction) | 16,9% | **34%** |
| Rapport | — | `omod/target/site/jacoco/` of CI-artifact `jacoco-report-pr-31` |

---

## Conclusie

Scope A: characterization tests (T1–T9) blijven het regressievangnet; extract unit tests dekken de gerefactorde methoden. Na refactor: 67 groene omod-tests, regressie-subset 58/58 groen, Quality Gate passed, CC-hotspot opgelost. Scope B volledig blijft rood; dat is bewust buiten scope.
