# Testresultaten baseline — OpenMRS HTML Form Entry

**Datum:** 15 juni 2026 · **Branch:** `implem-tests`  
**Scope:** A = PoC (`omod`), B = module-baseline — zie [`03-teststrategie.md`](03-teststrategie.md)

---

## Uitvoering

| | |
|---|---|
| Omgeving | Windows 10, Temurin JDK 8.0_482, Maven 3.9.12 |
| Scope A | `mvn -pl omod test verify` → **BUILD SUCCESS** (~15 s, **19 tests**) |
| Scope B | `mvn -pl api-tests test` → 529 tests, 71 rood (gedocumenteerd, niet gefixt) |

---

## Resultaten Scope A

| Klasse | Tests | Status |
|--------|-------|--------|
| `HtmlFormEncounterControllerTest` | 7 | ✅ |
| `HtmlFormAjaxValidationControllerTest` | 2 | ✅ |
| `HtmlFormEntryControllerTest` | 10 | ✅ (T1–T9, characterization) |
| **Totaal** | **19** | **✅** |

Traceability: [`03-teststrategie.md`](03-teststrategie.md) §7.4 · AI-verantwoording: [`05-verantwoording-ai-tests.md`](05-verantwoording-ai-tests.md)

---

## JaCoCo (hotspot)

| Metriek | Baseline | Na characterization tests |
|---------|----------|---------------------------|
| `HtmlFormEntryController` | 2,1% (3/143 lines) | **~53%** (76/143 lines) |
| Controller-pakket | 16,9% | zie `omod/target/site/jacoco/` |

---

## Conclusie

Scope A is **groen en refactor-ready**. Scope B blijft rood; dat is bewust buiten scope. **Volgende stap:** PoC Extract Class op `getFormEntrySession` met dezelfde tests als vangnet.
