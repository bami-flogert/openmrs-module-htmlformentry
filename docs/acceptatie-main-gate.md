# Acceptatie → main — merge-gate checklist

**Branch:** `acceptatie` → `main`  
**Leads:** Teamlid A (PR) · Teamlid B (doc-review)  
**Drempel rubric:** minimaal **55 pt** per onderdeel (max. 100)

Gebruik deze checklist vóór PR `acceptatie` → `main`. Alle items moeten ✅ zijn tenzij expliciet als optioneel gemarkeerd.

---

## Documenten — onderhoudbaarheid (B-track)

| # | Criterium | Status | Bewijs |
|---|-----------|--------|--------|
| 1 | [`05-ontwerp-refactoring.md`](onderhoudbaarheid/05-ontwerp-refactoring.md) compleet (as-is/to-be, alternatieven, NFR) | ☐ | |
| 2 | [`07-validatie-voor-na.md`](07-validatie-voor-na.md) — geen open placeholders; cijfers = [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md) | ☐ | 67 tests, ~63%, QG Passed |
| 3 | [`06-verantwoording-tooling.md`](onderhoudbaarheid/06-verantwoording-tooling.md) — PoC + AI + Sonar/CI gecombineerd | ☐ | |
| 4 | [`onderhoudbaarheidsrapport.md`](onderhoudbaarheidsrapport.md) §4.7 linkt naar `07-validatie` | ☐ | |
| 5 | NFR canonical: [`01-nfr-onderhoudbaarheid.md`](01-nfr-onderhoudbaarheid.md); stub in `onderhoudbaarheid/01` | ☐ | |
| 6 | Geen broken links tussen 03, 04, 05, 06, 07, rapport | ☐ | |

---

## Documenten — pipeline (A-track)

| # | Criterium | Status | Bewijs |
|---|-----------|--------|--------|
| 7 | [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md) — PR #31/#37 URLs, Sonar-tabel | ☐ | |
| 8 | [`03-teststrategie.md`](03-teststrategie.md) — exit-criteria §9.2, link naar `07` | ☐ | |
| 9 | CI: `unit-test` + `sonarcloud` jobs groen op acceptatie | ☐ | GitHub Actions |
| 10 | JaCoCo verify in `ci.yml`; `pom.xml` / `sonar-project.properties` correct | ☐ | |
| 11 | Branch protection: `build`, `unit-test`, SonarCloud Analysis op `main` | ☐ | [`sonarcloud-setup.md`](sonarcloud-setup.md), [`otap.md`](otap.md) |

---

## Code & tests

| # | Criterium | Status | Bewijs |
|---|-----------|--------|--------|
| 12 | PoC refactor `getFormEntrySession` op acceptatie | ☐ | PR #31 |
| 13 | T1–T9 + ExtractedMethodsTest groen | ☐ | 67 omod-tests |
| 14 | Regressie-subset 58/58 groen | ☐ | [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md) § Regressie-subset |
| 15 | Quality Gate Passed (SonarCloud) | ☐ | PR #31, #37 |

---

## Presentatie & CGI

| # | Criterium | Status | Bewijs |
|---|-----------|--------|--------|
| 16 | Presentatie-outline gereed | ☐ | [`08-presentatie-outline.md`](08-presentatie-outline.md) |
| 17 | Per teamlid min. één rubric-criterium geoefend | ☐ | CGI-vragen in § CGI-voorbereiding |
| 18 | Demo PoC + voor/na-metrieken voorbereid | ☐ | |

---

## Optioneel ("Goed"-niveau)

| # | Criterium | Status |
|---|-----------|--------|
| 19 | [`onderhoudbaarheid/README.md`](onderhoudbaarheid/README.md) index | ☐ |
| 20 | JaCoCo-screenshot in validatiedoc | ☐ |
| 21 | Security-track: backlog/riscos op acceptatie | ☐ |

---

## Merge-actie

1. Alle verplichte items (1–18) afvinken.
2. PR `acceptatie` → `main` openen (A leidt).
3. B reviewt docs in PR; geen force-push naar `main`.
4. Na merge: tag/release volgens OTAP indien van toepassing.

**Laatste update:** na merge `beroepsproduct-en-ontwerp` → `acceptatie` (commit `72f1ea3`).
