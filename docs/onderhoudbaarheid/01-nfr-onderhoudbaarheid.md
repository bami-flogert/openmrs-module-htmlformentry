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

- **NFR-T1** (≥60% JaCoCo coverage) uit `non-functionals.md` → ondersteunt **NFR-M5** (testbaarheid).
- **NFR-D1 / NFR-D3** (CI verplicht, security gates) → uitgebreid met **NFR-M7** (SonarCloud quality gate).

---

## Scope

| Niveau                        | Omschrijving                                                                                                        | Doel                                                                       |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| **Analyse**                   | Volledige Maven-module (`api`, `api-tests`, `omod`)                                                                 | Systematische baseline en SonarCloud-rapport                               |
| **PoC / verbetering**         | OMOD-laag — kritieke webcontrollers (voorlopig: `HtmlFormEntryController`, eventueel `HtmlFormEncounterController`) | Beheersbare refactoring binnen sprintcapaciteit                            |
| **Buiten PoC (documenteren)** | `FormEntrySession` en overige `api`-klassen                                                                         | Vermelden in analyse als toekomstig verbeterpunt; niet negeren in baseline |

> **Motivatie PoC-scope:** de OMOD-controllers vormen het web-ingangspunt voor form entry. `HtmlFormEntryController` bevat expliciet technische schuld (logica in `onSubmit`). De `api`-laag (`FormEntrySession`, ~1170 LOC) is waarschijnlijk de grootste hotspot, maar valt buiten de eerste PoC vanwege omvang en risico op regressie.

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

### NFR-M3 — Focus op verantwoordelijkheid per klasse (SRP)

**MoSCoW: MUST**

Klassen in PoC-scope mogen geen “god class” worden; webcontrollers bevatten geen domeinlogica.

**Meetbaar:**

- PoC-doelklasse (`HtmlFormEntryController`): **< 200 LOC** na Extract Class / Extract Method
- SonarCloud-maintainability rating op gewijzigde bestanden: geen verslechtering
- Geen nieuwe “brain-overload”-issues op PoC-bestanden

**Scope:** PoC-scope (OMOD-controllers + nieuw geëxtraheerde klassen)

---

### NFR-M4 — Beperkte code smells in gewijzigde code

**MoSCoW: MUST**

Technical debt moet afnemen of stabil blijven in het verbeterde gebied; geen extra smells introduceren.

**Meetbaar (SonarCloud):**

- **0 nieuwe** code smells op gewijzigde regels in PoC-PR (New Code)
- Maintainability rating module-breed: baseline vastgelegd; streef **B of beter** op PoC-scope na verbetering
- Aantal open smells in PoC-bestanden: **dalend** t.o.v. baseline

**Scope:** PoC-scope + New Code policy in SonarCloud

---

### NFR-M5 — Testbaarheid via coverage

**MoSCoW: SHOULD**

Gerefactorde code moet testbaar zijn en gedekt door geautomatiseerde tests.

**Meetbaar (JaCoCo + SonarCloud):**

- **≥ 60% line coverage** in PoC-scope (koppeling met **NFR-T1**)
- PoC-scope tests blijven groen in CI (`mvn -B -pl omod test verify`)
- Nieuwe of uitgebreide unit tests voor geëxtraheerde logica

**Scope:** PoC-scope (OMOD)

---

### NFR-M6 — Quality Gate als releasekriterium

**MoSCoW: MUST**

De pipeline moet merges blokkeren wanneer onderhoudbaarheidsdrempels niet worden gehaald.

**Meetbaar (SonarCloud Quality Gate):**

- Quality Gate **Passed** vereist voor merge naar `development` / `pre-release` / `acceptatie` / `main`
- Drempels in SonarCloud afgestemd op NFR-M1 t/m M5 (minimaal: geen new blocker/critical bugs, geen new critical smells, 0 new S3776 op changed code)
- `sonar.qualitygate.wait=true` in [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) (`sonarcloud`-job); CI faalt bij Failed gate
- Free plan: default **Sonar way** quality gate (geen custom gates); zie [`sonarcloud-setup.md`](../sonarcloud-setup.md)

**Scope:** module-breed · New Code policy voor PRs

---

### NFR-M7 — Statische analyse in elke PR

**MoSCoW: MUST**

Elke wijziging wordt automatisch geanalyseerd; handmatige kwaliteitscontrole alleen is onvoldoende.

**Meetbaar:**

- **100%** pull requests triggeren SonarCloud-analyse via `ci.yml` (`sonarcloud`-job)
- Analyse-resultaat (Quality Gate + link) zichtbaar in PR-checks **SonarCloud Analysis**
- Baseline-analyse op `main` minimaal één keer per sprint (trigger: `push` naar `main`)

**Scope:** CI/CD — [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml), config in [`sonar-project.properties`](../../sonar-project.properties)

---

### NFR-M8 — Analyseerbaarheid van beslissingen

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
| M4    | Code Smells, Maintainability Rating | 0 new blocker/critical smells    |
| M5    | Coverage, JaCoCo-report             | ≥ 60% on new code (PoC-fase)     |
| M6/M7 | Overall Quality Gate                | Fail CI bij Failed (`sonar.qualitygate.wait=true`) |

---

## Acceptatiecriteria onderhoudbaarheid

Het team voldoet aan deze NFR-set wanneer:

1. Baseline-metrieken op volledige module zijn vastgelegd.
2. SonarCloud Quality Gate actief in CI (`sonarcloud`-job) en merge blokkeerbaar via branch protection.
3. Minimaal **één** PoC-refactoring in PoC-scope is uitgevoerd conform ontwerp.
4. Voor/na-metrieken aantonen dat minimaal **NFR-M1, M3 en M4** zijn verbeterd of gehaald in PoC-scope.
5. PoC-scope tests draaien groen in CI; coverage in PoC-scope is gemeten en gedocumenteerd.

---

## Referenties

| Document    | Locatie                |
| ----------- | ---------------------- |
| Modulekeuze | `docs/module-keuze.md` |
| SonarCloud setup | `docs/sonarcloud-setup.md` |
| CI-workflow | `.github/workflows/ci.yml` |
