# Testresultaten baseline — OpenMRS HTML Form Entry

**Datum:** 17 juni 2026 · **Branch:** `merge/refactor-controller-into-acceptatie`  
**Scope:** A = PoC (`omod`), B = module-baseline — zie [`03-teststrategie.md`](03-teststrategie.md)

---

## Uitvoering

| | |
|---|---|
| Omgeving | Windows 10, Temurin JDK 8, Maven 3.9.x |
| Scope A | `mvn -pl omod test verify` → **BUILD SUCCESS** (34 tests) |
| Scope B | `mvn -pl api-tests test` → 529 tests, ~70 rood (gedocumenteerd, niet gefixt) |

---

## Resultaten Scope A

| Klasse | Tests | Status |
|--------|-------|--------|
| `HtmlFormEncounterControllerTest` | 7 | ✅ |
| `HtmlFormAjaxValidationControllerTest` | 2 | ✅ |
| `HtmlFormEntryControllerTest` | 10 | ✅ (T1–T9, characterization, ongewijzigd) |
| `HtmlFormEntryControllerExtractedMethodsTest` | 15 | ✅ (extract unit tests; resolveFormEntryContext via characterization) |
| **Totaal omod** | **34** | **✅** |

Traceability: [`03-teststrategie.md`](03-teststrategie.md) §7.4 · §7.2b · AI-verantwoording: [`05-verantwoording-ai-tests.md`](05-verantwoording-ai-tests.md)

---

## Resultaten na refactor-merge (api-tests)

| Klasse | Tests | Status |
|--------|-------|--------|
| `FormEntrySessionValidateNotModifiedSinceTimestampsTest` | 8 | ✅ |

---

## JaCoCo (hotspot)

| Metriek | Baseline | Na characterization tests |
|---------|----------|---------------------------|
| `HtmlFormEntryController` | 2,1% (3/143 lines) | **~53%** (76/143 lines) |
| Controller-pakket | 16,9% | zie `omod/target/site/jacoco/` |

---

## Conclusie

Scope A: characterization tests (T1–T9) blijven het regressievangnet; extract unit tests vullen de refactor-laag aan. Scope B blijft rood; dat is bewust buiten scope.
