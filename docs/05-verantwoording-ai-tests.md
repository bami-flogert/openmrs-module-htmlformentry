# Verantwoording AI — characterization tests

**Datum:** 15 juni 2026 · **Tool:** Cursor AI-agent · **Scope:** `HtmlFormEntryControllerTest` (T1–T9)

Gerelateerd: [`03-teststrategie.md`](03-teststrategie.md) §7.4 · [`06-verantwoording-tooling.md`](onderhoudbaarheid/06-verantwoording-tooling.md) (PoC, later)

---

## Context

Characterization tests op `HtmlFormEntryController.getFormEntrySession` (CC 52) vóór de geplande Extract Class-refactor. Alle paden uit de traceability-matrix (Must + Should) zijn geïmplementeerd.

---

## Rol mens vs. AI

| | AI (Cursor) | Mens (team) |
|---|-------------|-------------|
| **Wat** | Test-skeleton, pad-mapping uit flowchart §7.3, assertie-voorstellen, analyse OpenMRS-testsetup | Opdracht, review assertions, Maven-run, fixture-keuzes (`simplestForm.xml`, `TestingApplicationContext.xml`) |
| **Validatie** | — | `mvn -pl omod test verify` → **19/19 groen** |

---

## Tooling-keuze

AI versnelde het schrijven van 10 testmethoden en documentatie. **Correctheid** is gevalideerd door lokale Surefire + JaCoCo; de menselijke reviewer blijft eindverantwoordelijk (rubric: Realisatie & verantwoording).

---

## Beperkingen

- AI kent OpenMRS module-testcontext niet standaard; omod vereiste extra `TestingApplicationContext.xml` en Hibernate-mapping (niet triviaal af te leiden).
- Form-XML moest worden vereenvoudigd (`simplestForm.xml`) i.v.m. ontbrekende concepten in de standaard testdataset.

---

## Reflectie

AI is geschikt voor **repetitieve, specificatie-gedreven** tests (pad-tabel → JUnit). Voor **integratietest-setup** (Spring, Hibernate, datasets) is menselijke validatie essentieel. Dit past bij de projectkeuze: AI-ondersteund, mens reviewt en voert uit (`plan-teststrategie-goed.md` §8.1).

---

## Bewijs

| Item | Waarde |
|------|--------|
| Testklasse | `omod/src/test/java/.../HtmlFormEntryControllerTest.java` |
| Tests | 10 (T1–T9 incl. T4b) |
| Commando | `mvn -pl omod test verify` |
| JaCoCo hotspot | `HtmlFormEntryController` ~**53%** lines (was 2,1% baseline) |
