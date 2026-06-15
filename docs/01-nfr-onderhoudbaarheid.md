# Non-functionele eisen — Onderhoudbaarheid

**Tooling:** SonarCloud (SAST/kwaliteit), JaCoCo (testcoverage)

---

## Doel

Deze eisen beschrijven meetbare kwaliteitscriteria voor **onderhoudbaarheid** van de gekozen OpenMRS-module. Ze vormen de basis voor analyse, prioritering, ontwerp, PoC en validatie conform de LU2-projectopdracht en rubric onderhoudbaarheid.

Voor onderhoudbaarheid kijken we naar de ISO/IEC 25010, specifiek **modulariteit, herbruikbaarheid, analyseerbaarheid, wijzigbaarheid en testbaarheid**.

---

## Relatie met overige NFRs

| Document                                                           | Scope                                                                        |
| ------------------------------------------------------------------ | ---------------------------------------------------------------------------- |
| [`opdracht/non-functionals.md`](../../opdracht/non-functionals.md) | Security, compliance, CI/CD, risico                                          |
| **Dit document**                                                   | Onderhoudbaarheid (complexiteit, duplicatie, smells, coverage, quality gate) |

Koppelingen:

- **NFR-T1** (tests op geprioriteerde hotspots) uit `non-functionals.md` → ondersteunt **NFR-M4** (testbaarheid).
- **NFR-D1 / NFR-D3** (CI verplicht, security gates) → uitgebreid met **NFR-M6/M7** (SonarCloud quality gate).

> **Afwijking t.o.v. eerdere versie:** de rubric vereist geen percentage coverage. NFR-M4 en NFR-T1 zijn daarom herzien naar **risicogebaseerde dekking** van geprioriteerde hotspots (zie [`onderhoudbaarheidsrapport.md`](onderhoudbaarheidsrapport.md) §5). JaCoCo blijft als **meetwaarde** in baseline en validatie.

---

## Scope

| Niveau                        | Omschrijving                                                                                                        | Doel                                                                       |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| **Analyse**                   | Volledige Maven-module (`api`, `api-tests`, `omod`)                                                                 | Systematische baseline en SonarCloud-rapport                               |
| **PoC / verbetering**         | OMOD-laag — kritieke webcontrollers (voorlopig: `HtmlFormEntryController`, eventueel `HtmlFormEncounterController`) | Beheersbare refactoring binnen sprintcapaciteit                            |
| **Buiten PoC (documenteren)** | `FormEntrySession` en overige `api`-klassen                                                                         | Vermelden in analyse als toekomstig verbeterpunt; niet negeren in baseline |

> **Motivatie PoC-scope:** de OMOD-controllers vormen het web-ingangspunt voor form entry. `HtmlFormEntryController` bevat expliciet technische schuld (logica in `onSubmit`). De `api`-laag (`FormEntrySession`, ~1000 LOC) is waarschijnlijk de grootste hotspot, maar valt buiten de eerste PoC vanwege omvang en risico op regressie.

---

## Meetbare eisen (SMART + MoSCoW)

### NFR-M1 — Beheersbare methodcomplexiteit

**MoSCoW: MUST**

Methoden moeten begrijpelijk en testbaar blijven; hoge cognitieve complexiteit belemmert wijzigingen en vergroot foutkans.

**Meetbaar (SonarCloud):**

- Regel `java:S3776` (cognitive complexity): geen methoden met complexiteit **> 15** in PoC-scope na refactoring
- Geen **nieuwe** violations op gewijzigde code in PoC-PR
- Baseline: aantal violations `java:S3776` module-breed vastgelegd in analyse rapport

**Scope:** module-breed (baseline) · PoC-scope (na refactoring)

---

### NFR-M2 — Lage codeduplicatie

**MoSCoW: MUST**

Duplicatie vergroot onderhoudskosten: wijzigingen moeten op meerdere plekken worden doorgevoerd.

**Meetbaar (SonarCloud):**

- **Duplicated lines (%)** module-breed: streefwaarde **< 5%**; geen stijging t.o.v. baseline na PoC
- Top-duplicatieblokken gedocumenteerd in analyse (minimaal 3 voorbeelden indien aanwezig)

**Scope:** module-breed

---

### NFR-M3 — Beperkte code smells in gewijzigde code

**MoSCoW: MUST**

Technical debt moet afnemen of stabil blijven in het verbeterde gebied; geen extra smells introduceren.

**Meetbaar (SonarCloud):**

- **0 nieuwe** code smells op gewijzigde regels in PoC-PR (New Code)
- Maintainability rating module-breed: baseline vastgelegd; streef **B of beter** op PoC-scope na verbetering
- Aantal open smells in PoC-bestanden: **dalend** t.o.v. baseline

**Scope:** PoC-scope + New Code policy in SonarCloud

---

### NFR-M4 — Testbaarheid via kritieke punten

**MoSCoW: SHOULD**

Gerefactorde en nieuw geëxtraheerde code moet testbaar zijn en gedekt door **relevante** geautomatiseerde tests — niet door een blanket coverage-percentage.

**Meetbaar (tests + JaCoCo als meetwaarde):**

- **100% van de geprioriteerde PoC-hotspots** (minimaal prio 1: `HtmlFormEntryController.getFormEntrySession`) heeft minstens één geautomatiseerde test
- **100% line coverage** op **nieuw geëxtraheerde/testbare klassen** in de PoC-PR (geen harde drempel op het hele OMOD-pakket)
- Bestaande tests in **PoC-testscope** (`mvn -pl omod test`) blijven groen in CI
- JaCoCo line coverage in PoC-scope is **gemeten en gedocumenteerd** (baseline + voor/na); percentage is indicatief, geen acceptatiecriterium

**Scope:** PoC-scope (OMOD controllers + nieuwe extracties)

---

### NFR-M5 — Quality Gate als releasekriterium

**MoSCoW: MUST**

De pipeline moet merges blokkeren wanneer onderhoudbaarheidsdrempels niet worden gehaald.

**Meetbaar (SonarCloud Quality Gate):**

- Quality Gate **Passed** vereist voor merge naar `development` / `pre-release` / `main`
- Drempels in SonarCloud afgestemd op NFR-M1 t/m M4 (minimaal: geen new blocker/critical bugs, geen new critical smells, 0 new S3776 op changed code)
- `sonar.qualitygate.wait=true` in CI-workflow

**Scope:** module-breed · New Code policy voor PRs

---

### NFR-M6 — Statische analyse in elke PR

**MoSCoW: MUST**

Elke wijziging wordt automatisch geanalyseerd; handmatige kwaliteitscontrole alleen is onvoldoende.

**Meetbaar:**

- **100%** pull requests triggeren SonarCloud-analyse
- Analyse-resultaat (Quality Gate + link) zichtbaar in PR-checks
- Baseline-analyse op `main` minimaal één keer per sprint opnieuw vastgelegd

**Scope:** CI/CD (GitHub Actions)

---

### NFR-M7 — Analyseerbaarheid van beslissingen

**MoSCoW: SHOULD**

Kwaliteitsbevindingen en refactorkeuzes zijn traceerbaar naar metrieken en code-locaties.

**Meetbaar:**

- Analyse rapport bevat minimaal **8 metrieken** (bugs, smells, duplication %, coverage %, cognitive complexity, maintainability rating, LOC, quality gate status)
- Elke geprioriteerde verbetering verwijst naar minimaal **1 SonarCloud-issue** of metriek + bestand:regel
- Voor/na-metrieken bij PoC vastgelegd in validatiedocument

**Scope:** documentatie (`docs/onderhoudbaarheid/`)

---

## Tooling en drempels in SonarCloud

| NFR   | SonarCloud-meetpunt                 | Quality Gate (voorstel)          |
| ----- | ----------------------------------- | -------------------------------- |
| M1    | Cognitive Complexity, rule S3776    | 0 new violations op changed code |
| M2    | Duplicated Lines (%)                | Geen verslechtering (ratchet)    |
| M3    | Code Smells, Maintainability Rating | 0 new blocker/critical smells    |
| M4    | Coverage (meetwaarde), JaCoCo-report | Gedocumenteerd; geen harde QG-drempel op PoC-pakket |
| M6/M7 | Overall Quality Gate                | Fail CI bij Failed               |

---

## Acceptatiecriteria onderhoudbaarheid

Het team voldoet aan deze NFR-set wanneer:

1. Baseline-metrieken op volledige module zijn vastgelegd.
2. SonarCloud Quality Gate actief is in CI en merges kan blokkeren.
3. Minimaal **één** PoC-refactoring in PoC-scope is uitgevoerd conform ontwerp.
4. Voor/na-metrieken aantonen dat minimaal **NFR-M1, M3 en M4** zijn verbeterd of gehaald in PoC-scope (M4: hotspots getest, geëxtraheerde klassen gedekt).
5. PoC-testscope draait groen in CI; JaCoCo-coverage is gemeten en gedocumenteerd als meetwaarde.

---

## Referenties

| Document    | Locatie                |
| ----------- | ---------------------- |
| Modulekeuze | `docs/module-keuze.md` |
