# Validatie voor/na PoC — Regressie & kwaliteitsmetrics

**Taak:** LU2-38 — 4.9 Regressie & kwaliteitsmetrics  
**Branch:** `acceptatie` (PoC gemerged via PR #31)  
**Datum:** 18 juni 2026  
**Auteur:** Floris Bogers  
**Scope:** OMOD webcontrollers — `HtmlFormEntryController.getFormEntrySession` (PoC-hotspot)

---

## 1. Doel

Dit document toont aan dat:

1. de bestaande tests na de PoC-refactor nog groen zijn (**regressie**);
2. de kwaliteitsmetrics aantoonbaar zijn verbeterd ten opzichte van de baseline (**voor/na**).

Referenties: [`03-teststrategie.md`](03-teststrategie.md) §9 · [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md) · [`onderhoudbaarheidsrapport.md`](onderhoudbaarheidsrapport.md) §4 · [`05-ontwerp-refactoring.md`](onderhoudbaarheid/05-ontwerp-refactoring.md)

---

## 2. Regressie — testsuite na PoC

### 2.1 PoC-scope (OMOD) — primaire testruimte

Uitvoering: `mvn -pl omod -am test verify`

| Testklasse | Tests | Status |
|------------|-------|--------|
| `HtmlFormEncounterControllerTest` | 7 | groen |
| `HtmlFormAjaxValidationControllerTest` | 2 | groen |
| `HtmlFormEntryControllerTest` | 13 | groen (T1–T9 characterization) |
| `HtmlFormEntryControllerExtractedMethodsTest` | 18 | groen (extract unit tests) |
| Overige omod-tests | 27 | groen |
| **Totaal omod** | **67** | **BUILD SUCCESS** |

Alle characterization-tests (T1–T9) zijn groen gebleven; `HtmlFormEntryControllerExtractedMethodsTest` dekt de geëxtraheerde methoden (`resolveMode`, `resolveFormEntryContext`, `resolvePatientForSession`, `buildFormEntrySession`, …).

**CI-bewijs:** [PR #31 CI-run](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27682116331) — unit-test job groen.

### 2.2 Regressie-subset — api-tests (module-baseline)

Vijf representatieve groene testklassen uit `api-tests` (zie [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md) § Regressie-subset). De volledige `api-tests`-suite heeft bekende instabiliteit (~70 rode tests module-breed); de subset toont aan dat de kern functioneel blijft.

| # | Testklasse | Tests | Domein | Status |
|---|------------|-------|--------|--------|
| 1 | `ObsTagTest` | 37 | Obs/submit-flow in form entry | groen |
| 2 | `HtmlFormTest` | 3 | HtmlForm-kern | groen |
| 3 | `FormEntrySessionTest` | 4 | Session-lifecycle | groen |
| 4 | `HtmlFormEntryServiceTest` | 6 | Service-laag submit | groen |
| 5 | `FormEntrySessionValidateNotModifiedSinceTimestampsTest` | 8 | PoC Move Method (concurrency-check) | groen |
| | **Totaal subset** | **58** | | **BUILD SUCCESS** |

**Conclusie regressie:** PoC-scope volledig groen (67/67); api-tests subset **58/58 groen**. Geen regressie geïntroduceerd door de refactor.

---

## 3. Kwaliteitsmetrics voor/na

### 3.1 Cognitive Complexity (NFR-M1)

**Eis:** geen methoden > CC 15 in PoC-scope na refactoring (SonarCloud rule `java:S3776`).

| Methode | Voor PoC (baseline) | Na PoC (refactor) | Drempel | Status |
|---------|--------------------|--------------------|---------|--------|
| `getFormEntrySession` | **CC 52** | **≤ 15** (geen S3776) | ≤ 15 | **voldaan** |
| `resolveMode` *(nieuw)* | — | CC 1 | ≤ 15 | voldaan |
| `resolveHtmlFormForEncounter` *(nieuw)* | — | CC 3 | ≤ 15 | voldaan |
| `resolveEncounterById` *(nieuw)* | — | CC 2 | ≤ 15 | voldaan |
| `resolveFormEntryContext` *(nieuw)* | — | CC 8 | ≤ 15 | voldaan |
| `resolvePatientForSession` *(nieuw)* | — | CC 5 | ≤ 15 | voldaan |
| `buildFormEntrySession` *(nieuw)* | — | CC ≤ 10 | ≤ 15 | voldaan |
| `handleSubmit` *(buiten PoC)* | CC 22 | CC 22 | — | secundair risico |

**Beoordeling M1:** **voldaan** — PoC-hotspot CC daalde van **52 → geen S3776-violation**; 0 new S3776 op changed code (SonarCloud PR #31). `handleSubmit` (CC 22) blijft buiten PoC-scope; gedocumenteerd als vervolg-refactor in [`05-ontwerp-refactoring.md`](onderhoudbaarheid/05-ontwerp-refactoring.md).

### 3.2 Code smells (NFR-M3)

**Eis:** 0 nieuwe blocker/critical smells op gewijzigde code; maintainability rating B of beter.

| Metriek | Voor PoC | Na PoC | Drempel | Status |
|---------|----------|--------|---------|--------|
| Brain Method smell `getFormEntrySession` | aanwezig (CC 52) | **opgelost** | — | voldaan |
| New code smells (changed code) | — | **5 Major** (geen blocker/critical) | 0 new blocker/critical | **voldaan** |
| Maintainability rating | A | **A** | B of beter | voldaan |

De vijf Major smells zijn trade-off van Extract Method + Move Method (4× `FormEntrySession`, 1× `buildFormEntrySession` 9 parameters). Geen gedragsregressie; zie [`06-verantwoording-tooling.md`](onderhoudbaarheid/06-verantwoording-tooling.md) §4.

**Controller-omvang (architectuur, geen aparte NFR-drempel):** `HtmlFormEntryController` ~**567 LOC** na refactor — boven het oorspronkelijke ontwerpdoel (< 200 LOC via Extract Class). **Bewust geaccepteerde afwijking:** Extract Method i.p.v. Extract Class; motivatie in [`05-ontwerp-refactoring.md`](onderhoudbaarheid/05-ontwerp-refactoring.md) §2.3.

### 3.3 Testbaarheid — hotspot gedekt (NFR-M4)

**Eis:** geprioriteerde PoC-hotspots hebben geautomatiseerde tests; geëxtraheerde onderdelen gedekt; JaCoCo gemeten en gedocumenteerd.

| Metriek | Voor PoC | Na PoC | Status |
|---------|----------|--------|--------|
| `getFormEntrySession` gedekt door test | nee | ja — T1–T9 + ExtractedMethodsTest | **voldaan** |
| Geëxtraheerde methoden / `FormEntryResolution` | n.v.t. | 18 extract tests + **100%** coverage inner class | **voldaan** |
| JaCoCo `HtmlFormEntryController` | 2,1% (3/143) | **~63%** (99/157 lines) | **voldaan** |
| JaCoCo CI-artifact | — | `jacoco-report-pr-31`; verify PR #37 | gedocumenteerd |

**Beoordeling M4:** **voldaan** — geen aparte top-level `FormEntryRequestResolver`; inner class `FormEntryResolution` + `HtmlFormEntryControllerExtractedMethodsTest` dekken de extractie. JaCoCo na volledige PoC: **~63%** (niet ~53% baseline na alleen characterization tests). Bron: [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md) § SonarCloud + JaCoCo; artifact [PR #31](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27682116331) / verify [PR #37](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27749957935).

### 3.4 Duplicated lines (NFR-M2)

**Eis:** < 5% module-breed; geen stijging t.o.v. baseline.

| Metriek | Baseline | Na PoC | Drempel | Status |
|---------|----------|--------|---------|--------|
| Duplicated lines (module-breed) | 1,9% | stabiel (refactor extraheert, dupliceert niet) | < 5% | voldaan |
| Duplicated lines (OMOD) | 1,7% | ~1,7% | < 5% | voldaan |

### 3.5 Quality Gate en SAST (NFR-M5/M6)

**Eis (NFR-M5):** SonarCloud Quality Gate `Passed` vereist voor merge naar `acceptatie`.  
**Eis (NFR-M6):** elke PR triggert SonarCloud-analyse; resultaat zichtbaar in PR-checks.

| Item | Status | Bewijs |
|------|--------|--------|
| SonarCloud op elke PR | ✅ | `.github/workflows/ci.yml` — `sonarcloud`-job |
| `sonar.qualitygate.wait=true` | ✅ | pipeline faalt bij FAILED |
| Quality Gate PR #31 (PoC-merge) | **PASSED** | [CI-run 27682116331](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27682116331) |
| Quality Gate PR #37 (JaCoCo-fix) | **PASSED** | [CI-run 27749957935](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27749957935) |
| Sonar-dashboard | | [PR #31](https://sonarcloud.io/dashboard?id=bami-flogert_openmrs-module-htmlformentry&pullRequest=31) · [PR #37](https://sonarcloud.io/dashboard?id=bami-flogert_openmrs-module-htmlformentry&pullRequest=37) |

**Beoordeling M5/M6:** **voldaan** — structureel geborgd in CI; beide relevante PR-runs tonen Quality Gate **PASSED**.

---

## 4. Samenvatting voor/na

| NFR | Metriek | Voor | Na | Drempel | Status |
|-----|---------|------|----|---------|--------|
| M1 | CC `getFormEntrySession` | 52 | geen S3776 (≤ 15) | ≤ 15 | **voldaan** |
| M1 | New S3776 op changed code | 1 | **0** | 0 new | **voldaan** |
| M2 | Duplicated lines (OMOD) | 1,7% | ~1,7% | < 5% | voldaan |
| M3 | New blocker/critical smells | — | **0** | 0 | **voldaan** |
| M3 | New Major smells | — | 5 (acceptabel) | geen blocker | voldaan |
| M4 | Hotspot + extract gedekt | nee | ja (T1–T9 + 18 extract) | hotspots | **voldaan** |
| M4 | JaCoCo controller | 2,1% | **~63%** | gedocumenteerd | **voldaan** |
| M5/M6 | Quality Gate | — | **Passed** (PR #31, #37) | Passed | **voldaan** |
| — | Regressie omod | baseline | **67 groen** | 100% | **voldaan** |
| — | Regressie-subset api-tests | baseline | **58 groen** | representatief | **voldaan** |

---

## 5. Traceerbaarheid

| Verbetering | NFR | Bewijs |
|-------------|-----|--------|
| Extract Method op `getFormEntrySession` | M1, M3, M4 | PR #31 merge → `acceptatie` |
| Extract unit tests | M4 | `HtmlFormEntryControllerExtractedMethodsTest` (18 tests) |
| Regressie PoC-scope | — | [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md) § Scope A |
| Regressie-subset api-tests | — | [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md) § Regressie-subset |
| Sonar/CI voor/na | M5, M6 | [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md) § SonarCloud |
| Ontwerpkeuze Extract Method | M1, M3, M4 | [`05-ontwerp-refactoring.md`](onderhoudbaarheid/05-ontwerp-refactoring.md) |
| AI + tooling reflectie | — | [`06-verantwoording-tooling.md`](onderhoudbaarheid/06-verantwoording-tooling.md) |

---

## 6. Tooling-verantwoording (PoC-refactor)

| Activiteit | Tool / aanpak | Menselijke validatie |
|------------|---------------|----------------------|
| Extract Method op `getFormEntrySession` | Cursor AI-agent (Composer) | Review methodgrenzen, naamgeving, JavaDoc |
| Unit tests geëxtraheerde methoden | Cursor AI-agent — test-skeletons | Review assertions; 67/67 omod groen |
| CC- en smell-meting | SonarCloud CI | PR #31/#37 Quality Gate PASSED |

Volledig AI-verantwoordingsdocument: [`05-verantwoording-ai-tests.md`](05-verantwoording-ai-tests.md) · uitgebreide reflectie: [`06-verantwoording-tooling.md`](onderhoudbaarheid/06-verantwoording-tooling.md)
