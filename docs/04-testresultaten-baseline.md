# Testresultaten baseline — OpenMRS HTML Form Entry

**Datum:** 15 juni 2026 · **Commit:** `e8c6d4b` · **Branch:** `implem-tests`  
**Scope:** A = PoC (`omod`), B = module-baseline (`api`, `api-tests`) — zie [`03-teststrategie.md`](03-teststrategie.md)

---

## Uitvoering

| | |
|---|---|
| Omgeving | Windows 10, Temurin JDK 8.0_482, Maven 3.9.12, TZ `America/New_York` |
| Scope A | `mvn -pl omod test verify` → **BUILD SUCCESS** (~11 s) |
| Scope B | `mvn -pl api-tests test` → **BUILD FAILURE**; `mvn test` stopt vroeg bij `api-1.10` |

Surefire: `omod/target/surefire-reports/`, `api-tests/target/surefire-reports/`  
JaCoCo: `omod/target/site/jacoco/`

---

## Resultaten

| Scope | Module / klasse | Run | Groen | Rood | Status |
|-------|-----------------|-----|-------|------|--------|
| **A** | `omod` (2 klassen) | 9 | 9 | 0 | ✅ |
| **B** | `api` | 43 | 41 | 0 (+2 skip) | ✅ |
| **B** | `api-1.10` | 4 | 1 | 3 | ❌ NPE `FormEntrySession` |
| **B** | `api-tests` | 529 | 457 | 71 (70 err + 1 fail) | ❌ |

**Scope B — fouten:** vrijwel allemaal NPE in `FormEntrySession.<init>`; 1 locale-failure in `HtmlFormEntryGeneratorTest`. Grootste bron: `RegressionTest` (26 errors). Niet fixen in deze sprint.

**Scope A — gap:** `HtmlFormEntryControllerTest` ontbreekt nog (characterization tests vóór PoC).

---

## JaCoCo (Scope A, controller-pakket)

| Metriek | Waarde |
|---------|--------|
| Controller-pakket | **16,9%** (106/626 lines) — consistent met [`onderhoudbaarheidsrapport.md`](onderhoudbaarheidsrapport.md) |
| PoC-hotspot `HtmlFormEntryController` | **2,1%** (3/143 lines) |
| OMOD totaal | 17,0% (106/622 lines) |

---

## Conclusie

PoC-scope is **groen en CI-ready**; module-baseline is **rood maar gedocumenteerd**. Gerichte tests op `getFormEntrySession` zijn de juiste vervolgstap — niet 70+ regressiefouten repareren.

**Volgende stap:** `HtmlFormEntryControllerTest` (P0-paden T1–T5, T7 in `03-teststrategie.md` §7.4).
