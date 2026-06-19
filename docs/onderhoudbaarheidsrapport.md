# Onderhoudbaarheidsrapport — Baseline-analyse

**Baseline-datum:** 2026-06-11  
**Commit:** `c67d09b` (merge PR #9 — OTAP)

## 1. Doel en scope

### Doel

Dit rapport legt de **baseline** van de onderhoudbaarheid van de gekozen OpenMRS-module vast. De bevindingen vormen input voor prioritering, ontwerp, PoC en validatie.

We beoordelen onderhoudbaarheid aan de hand van **ISO/IEC 25010**, specifiek:


| Eigenschap            | Wat we meten in deze analyse                                                |
| --------------------- | --------------------------------------------------------------------------- |
| **Modulariteit**      | Klassengrootte, verantwoordelijkheden per laag, koppeling controller ↔ api  |
| **Herbruikbaarheid**  | Duplicatie (NFR-M2), herhaalde logica in controllers                        |
| **Analyseerbaarheid** | Traceerbare metrieken, SonarCloud-issues, reproduceerbare baseline (NFR-M7) |
| **Wijzigbaarheid**    | Cognitieve complexiteit (NFR-M1), code smells (NFR-M3)                      |
| **Testbaarheid**      | Line coverage JaCoCo (NFR-M4), testbaarheid complexe methodes               |


### Scope-niveaus


| Niveau                     | Omschrijving                                                                                     | Rol in dit rapport                            |
| -------------------------- | ------------------------------------------------------------------------------------------------ | --------------------------------------------- |
| **Analyse (module-breed)** | Volledige Maven-module: `api`, `api-tests`, `omod` (+ versie-specifieke `api-` submodules)       | Baseline-metrieken en hotspots                |
| **PoC-scope**              | OMOD webcontrollers — primair `HtmlFormEntryController`, eventueel `HtmlFormEncounterController` | Detailanalyse + prioritering voor verbetering |
| **Buiten PoC**             | `FormEntrySession` en overige `api`-klassen                                                      | Meegenomen in het begin.                      |


> **Motivatie PoC-focus:** OMOD-controllers zijn het web-ingangspunt voor form entry. `HtmlFormEntryController` bevat expliciet technical depth (logica hoort in `FormEntrySession`, zie TODO in broncode). `FormEntrySession` (~1228 LOC) is waarschijnlijk de grootste hotspot, maar valt buiten de eerste PoC vanwege omvang en regressierisico.

---

## 2. Methodologie en tooling

### Gebruikte tools


| Tool               | Doel                                                                                  | NFR-koppeling                   |
| ------------------ | ------------------------------------------------------------------------------------- | ------------------------------- |
| **SonarCloud**     | SAST, cognitive complexity, smells, duplication, maintainability rating, quality gate | NFR-M1, M2, M3, M5, M6, M7      |
| **JaCoCo**         | Line coverage via Maven                                                               | NFR-M4 (koppeling NFR-T1 ≥ 60%) |
| **Maven**          | Build en testuitvoering (`mvn test`)                                                  | NFR-M4, M6                      |
| **GitHub Actions** | CI-pipeline                                                                           | NFR-M5, M6                      |


### Drempelwaarden (onze eigen NFR-set)


| Metriek                          | Drempel                            | Bron                                 |
| -------------------------------- | ---------------------------------- | ------------------------------------ |
| Cognitive complexity per methode | ≤ 15                               | NFR-M1, SonarCloud rule `java:S3776` |
| Duplicated lines (%)             | < 5% (streef)                      | NFR-M2                               |
| Code smells (new code)           | 0 nieuwe smells op gewijzigde code | NFR-M3                               |
| Line coverage (PoC-scope)        | ≥ 60%                              | NFR-M4                               |
| Quality Gate                     | Passed vereist voor merge          | NFR-M5                               |


### Reproduceerbaarheid

```bash
# Tests + coverage (JaCoCo)
mvn clean test

# SonarCloud
# na elke pr te zien.
```

---

## 3. Samenvatting baseline-metrieken


| #   | Metriek                                     | Module-breed          | PoC-scope (OMOD controllers) | Drempel (NFR)           | Status                                            |
| --- | ------------------------------------------- | --------------------- | ---------------------------- | ----------------------- | ------------------------------------------------- |
| 1   | **Bugs** (open)                             | 26                    | 4                            | 0 new blocker/critical  | ✅ (medium of low)                                 |
| 2   | **Code smells** (open)                      | **1,230**             | **59**                       | Dalend in PoC-bestanden | ❌                                                 |
| 3   | **Duplicated lines (%)**                    | **1.9%**              | **1.7%**                     | < 5%                    | ✅                                                 |
| 4   | **Line coverage (%)**                       | **16,4%** (1642/9987) | 1,9% (207/10619)             | ≥ 60% (PoC)             | ❌ (api-1.10 heeft 86,7% (397/458))                |
| 5   | **Cognitive complexity violations** (S3776) | **97**                | **5**                        | ≤ 15 per methode        | ❌                                                 |
| 6   | **Maintainability rating**                  | A                     | A                            | B of beter (PoC)        | ✅                                                 |
| 7   | **Lines of code (nLines)**                  | **35.544**            | **1.261**                    | NVT                     | NVT                                               |
| 8   | **Quality Gate status**                     | Voor alle nieuwe code | NVT                          | Passed                  | ✅ (als het faalt, dan code aanpassen, voor merge) |


> line coverage is getest met: `mvn clean verify "-Dmaven.test.failure.ignore=true"` omdat de init al van de 529 tests, 73 errors en 1 failure heeft.

### Korte interpretatie

- **Complexiteit:** module-breed **97** S3776-violations; in PoC-scope (OMOD) **5** methodes boven drempel 15. Dit is het zwaarste baseline-probleem voor wijzigbaarheid en testbaarheid.
- **Duplicatie:** Redelijk laag. Zie §4.2 voor beperking rond SonarCloud quality gate.
- **Coverage:** Laag precentage. Complexe controller-methodes zijn moeilijk te unit-testen zonder extractie of mocking van HTTP/session-laag.
- **Quality Gate:** Voor nu de default (vanwege free plan)
  - Reliability rating is **A**
  - Security rating is **A**
  - Maintainability rating is **A**
  - Security Hotspots Reviewed is **100%**
  - Coverage is greater than or equal to **80.0%**
  - Duplicated Lines (%) is less than or equal to **3.0%**

---

## 4. Bevindingen per NFR

### 4.1 NFR-M1 — Beheersbare methodcomplexiteit (MUST)

**Eis:** geen methoden met cognitive complexity > 15 in PoC-scope na refactoring; baseline module-breed vastgelegd; geen nieuwe S3776-violations op gewijzigde code.

**SonarCloud-regel:** `java:S3776` — drempel **15** (SonarCloud-standaard).

#### Baseline


| Scope           | Aantal S3776-violations |
| --------------- | ----------------------- |
| Module-breed    | **97**                  |
| OMOD (PoC-laag) | **5**                   |


#### OMOD — methodes boven drempel (PoC-scope)


| Bestand                                     | Methode               | CC     | Drempel | Overtreding | PoC-kandidaat     |
| ------------------------------------------- | --------------------- | ------ | ------- | ----------- | ----------------- |
| `omod/.../HtmlFormEntryController.java`     | `getFormEntrySession` | **52** | 15      | +37         | **Ja, primair**   |
| `omod/.../PopupWidgetController.java`       | `personSearch`        | **55** | 15      | +40         | Nee (buiten PoC)  |
| `omod/.../HtmlFormEncounterController.java` | `getValue`            | **35** | 15      | +20         | **Ja, secundair** |
| `omod/.../HtmlFormSearchController.java`    | `conceptSearch`       | **29** | 15      | +14         | Nee (buiten PoC)  |
| `omod/.../HtmlFormFromFileController.java`  | `handleRequest`       | **20** | 15      | +5          | Nee (buiten PoC)  |


**Volledige paden:**

- `omod/src/main/java/org/openmrs/module/htmlformentry/web/controller/HtmlFormEntryController.java`
- `omod/src/main/java/org/openmrs/module/htmlformentry/web/controller/PopupWidgetController.java`
- `omod/src/main/java/org/openmrs/module/htmlformentry/web/controller/HtmlFormEncounterController.java`
- `omod/src/main/java/org/openmrs/module/htmlformentry/web/controller/HtmlFormSearchController.java`
- `omod/src/main/java/org/openmrs/module/htmlformentry/web/controller/HtmlFormFromFileController.java`

#### Detail: `HtmlFormEntryController.getFormEntrySession` (CC 52)

methode centraliseert request-parsing, sessie-opbouw en form-entry flow; meerdere vertakkingen op request-parameters en modus. De klasse bevat zelf een TODO: te veel logica hoort in `FormEntrySession`.

**ISO 25010:** wijzigbaarheid, modulariteit (controller doet te veel)

#### Module-breed aanvullende hotspot (could have)


| Bestand / klasse                | Indicatie                   | CC / LOC      |
| ------------------------------- | --------------------------- | ------------- |
| `api/.../FormEntrySession.java` | Grootste klasse in api-laag | ~**1228** LOC |


---

### 4.2 NFR-M2 — Lage codeduplicatie (MUST)

**Eis:** duplicated lines (%) module-breed < 5%; top-duplicatieblokken gedocumenteerd (min. 3 indien aanwezig); geen stijging t.o.v. baseline na PoC. Bij < 5% en geen opvallende blokken volstaat de percentage-meting.


| Metriek                           | Waarde baseline | Drempel NFR | Status |
| --------------------------------- | --------------- | ----------- | ------ |
| Duplicated lines (%) module-breed | 1.9%            | < 5%        | ✅      |
| Duplicated lines (%) OMOD         | 1.7%            | < 5%        | ✅      |


Verbeteringen: geen veranderingen, weinig duplicatie in codebase. Gecontroleerd in SonarCloud; geen significante blokken gevonden

---

### 4.3 NFR-M3 — Beperkte code smells in gewijzigde code (MUST)

**Eis:** 0 nieuwe smells op new code; maintainability rating baseline vastgelegd; streef B of beter in PoC-scope; dalend aantal smells in PoC-bestanden.


| Metriek                | Module-breed | PoC-bestanden | Drempel                |
| ---------------------- | ------------ | ------------- | ---------------------- |
| Open code smells       | **1.230**    | **59**        | Dalend t.o.v. baseline |
| Maintainability rating | **A**        | **A**         | B of beter             |


#### Voorbeeld-smells (representatief)

PoC-scope telt **59** open smells; onderstaand zijn **drie representatieve voorbeelden** — geen exhaustieve lijst. De bulk hangt samen met dezelfde patronen (lange methodes, mixed responsibilities) als in §4.1.


| #   | Bestand                                     | Indicatie                                  | Smell-type (SonarCloud)      | Ernst |
| --- | ------------------------------------------- | ------------------------------------------ | ---------------------------- | ----- |
| 1   | `HtmlFormEntryController.java`              | `getFormEntrySession` / `onSubmit` (CC 52) | Brain Method / hoge CC       | Major |
| 2   | `HtmlFormEncounterController.java`          | `getValue` (CC 35)                         | Brain Method                 | Major |
| 3   | `HtmlFormEntryController.java` (klasniveau) | TODO: logica hoort in `FormEntrySession`   | Mixed responsibilities (SRP) | Info+ |


#### Focus: verantwoordelijkheid per klasse (Single Responsibility)

Controllers in OMOD vertonen **mixed responsibilities**: HTTP-afhandeling, request-mapping, sessiebeheer en domeinlogica in één methode. Dit versterkt smells en complexiteit, met name in `HtmlFormEntryController`.

---

### 4.4 NFR-M4 — Testbaarheid via coverage (SHOULD)

**Eis:** ≥ 60% line coverage in PoC-scope; bestaande tests groen in CI; unit tests voor geëxtraheerde logica.


| Scope                     | Line coverage (%)     | Drempel | Status |
| ------------------------- | --------------------- | ------- | ------ |
| Module-breed              | **16,4%** (1642/9987) | —       | —      |
| OMOD / PoC-bestanden      | **1,9%** (207/10619)  | ≥ 60%   | ❌      |
| `HtmlFormEntryController` | **2,1%** (3/143)      | ≥ 60%   | ❌      |


> **JaCoCo-run:** `mvn clean verify -Dmaven.test.failure.ignore=true` (2026-06-11).  
> PoC-totaal in §3 gebruikt de hele `omod`-module als denominator (10.619 lijnen incl. DWR-service en testcode). Alleen de **controller-package** scoort **16,9%** (106/626) — nog steeds ver onder de drempel.

#### Testbaarheidsbelemmeringen (koppeling M1 → M4)


| Methode               | CC  | Testbaarheid | Reden                                                                |
| --------------------- | --- | ------------ | -------------------------------------------------------------------- |
| `getFormEntrySession` | 52  | Laag         | Geen dedicated testklasse; zware HTTP/sessie-afhankelijkheid         |
| `getValue`            | 35  | Middel       | `HtmlFormEncounterControllerTest` (7 tests) dekt ~88% van die klasse |
| `personSearch`        | 55  | Laag         | Geen tests; zelfde HTTP/search-patroon als andere widget-controllers |


**Bestaande tests:** `api-tests` — **52** testklassen, **529** tests totaal (api-laag/form-tags). PoC-gerelateerd in `omod`: `HtmlFormEncounterControllerTest` (7 tests ✅), `HtmlFormAjaxValidationControllerTest` (2 tests ✅); **geen** tests voor `HtmlFormEntryController`.  
**CI-status `mvn test`:** **Rood** — 529 tests, 70 errors, 1 failure (2026-06-11); coverage wel gegenereerd via `failure.ignore=true`.

---

### 4.5 NFR-M5 — Quality Gate als releasekriterium (MUST)

**Eis:** Quality Gate Passed vereist voor merge naar `development` / `pre-release` / `main`; drempels afgestemd op NFR-M1 t/m M5; `sonar.qualitygate.wait=true` in CI.


| Aspect                    | Huidige status                              | Gewenst (NFR)    |
| ------------------------- | ------------------------------------------- | ---------------- |
| Quality Gate overall      | **Passed** (want er is nog geen *new code*) | Passed           |
| New blocker/critical bugs | 0 (new code policy)                         | 0                |
| New critical smells       | 0 (new code policy)                         | 0                |
| Coverage on new code      | Passed (SonarCloud-drempel 80% op new code) | ≥ 60%            |
| S3776 op changed code     | **5 open in OMOD** (baseline)               | 0 new violations |


**CI-workflow:** SonarCloud-stap is aanwezig in `.github/workflows/ci.yml` (`sonarcloud`-job, `sonar.qualitygate.wait=true`). De `deploy.yml` draait nog met `-DskipTests`, maar kwaliteitscontrole loopt via de aparte `ci.yml`.

- `.github/workflows/ci.yml` — `unit-test` + `sonarcloud` jobs, Quality Gate geborgd
- `.github/workflows/deploy.yml` — `mvn package -DskipTests`, geen SonarCloud (bewust)

---

### 4.6 NFR-M6 — Statische analyse in elke PR (MUST)

**Eis:** 100% PRs triggeren SonarCloud; resultaat zichtbaar in PR-checks; baseline op `main` minimaal 1× per sprint vastgelegd.


| Aspect                              | Status                                                               |
| ----------------------------------- | -------------------------------------------------------------------- |
| SonarCloud op elke PR               | ✅ `ci.yml` triggert op `pull_request` en `push`                     |
| Quality Gate zichtbaar in PR-checks | ✅ `sonar.qualitygate.wait=true` in `ci.yml` (`sonarcloud`-job)      |
| Baseline op main (dit rapport)      | ✅ vastgelegd op **2026-06-11** (commit `c67d09b`)                    |
| Branch protection                   | Zie [`sonarcloud-setup.md`](sonarcloud-setup.md) voor status         |

---

### 4.7 NFR-M7 — Analyseerbaarheid van beslissingen (SHOULD)

**Eis:** rapport met ≥ 8 metrieken; elke geprioriteerde verbetering verwijst naar ≥ 1 Sonar-issue of metriek + bestand:regel; voor/na-metrieken bij PoC in validatiedocument.


| Vereiste                                     | Status in dit document                     |
| -------------------------------------------- | ------------------------------------------ |
| ≥ 8 metrieken in §3                          | ✅ alle 8 metrieken ingevuld                |
| Traceerbaarheid hotspots → metriek + locatie | ✅ OMOD CC-tabel met bestand, methode en CC |
| Voor/na-metrieken PoC                        | ✅ zie [`07-validatie-voor-na.md`](07-validatie-voor-na.md)         |


---

## 5. Hotspots en prioritering

Input voor het verbeteronderzoek (rubric rij 3). Prioritering op **impact** (effect op onderhoudbaarheid/gebruik) en **effort** (omvang, tests, regressierisico).


| Prio  | Hotspot                                               | NFR        | Impact                                                     | Effort                                                     | Onderbouwing                                              |
| ----- | ----------------------------------------------------- | ---------- | ---------------------------------------------------------- | ---------------------------------------------------------- | --------------------------------------------------------- |
| **1** | `HtmlFormEntryController.getFormEntrySession` (CC 52) | M1, M3, M4 | **Hoog** — web-ingang form entry; expliciete TODO in code  | **Middel** — beperkt tot OMOD, delegeer naar bestaande api | Hoogste PoC-waarde; directe link met known technical debt |
| **2** | `HtmlFormEncounterController.getValue` (CC 35)        | M1, M4     | **Middel–hoog** — encounter flow                           | **Middel**                                                 | Secundaire PoC-kandidaat binnen scope                     |
| **3** | `FormEntrySession` (~1228 LOC)                        | M1, M3, M4 | **Zeer hoog** — kern domeinlogica                          | **Hoog** — regressierisico, veel callers                   | Buiten PoC; wel vermelden in baseline                     |
| **4** | `PopupWidgetController.personSearch` (CC 55)          | M1         | **Middel** — hulp-UI                                       | **Laag–middel**                                            | Buiten PoC; lagere business-prioriteit                    |
| **5** | Codeduplicatie (§4.2)                                 | M2         | **Laag** — 1,9% module-breed, 1,7% OMOD; onder NFR-drempel | **NVT** — geen actie in PoC                                | Geen prioriteit; baseline voldoet al aan NFR-M2           |


**PoC-besluit (voorstel):** start met **prioriteit 1** (`HtmlFormEntryController`); prioriteit 2 alleen indien sprintcapaciteit.

---

## 6. Buiten PoC-scope (bewust gedocumenteerd)

Deze onderdelen vallen **niet** in de eerste PoC, maar zijn wél meegenomen in de module-baseline:


| Onderdeel                                                                         | Reden buiten PoC                                    | Baseline-indicatie                                                                                          |
| --------------------------------------------------------------------------------- | --------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `FormEntrySession` + overige `api`-klassen                                        | ~1228 LOC, centraal in domein, hoog regressierisico | Heeft zelf een CC van 30. `applyAction()` heeft 147 CC. `getSetLastSubmissionFieldsJavascript()` is 92 CC.  |
| `PopupWidgetController`, `HtmlFormSearchController`, `HtmlFormFromFileController` | Buiten kritieke PoC-focus                           | CC resp. 55, 29, 20 (§4.1)                                                                                  |
| Versie-specifieke `api-` submodules                                               | Parallelle API-implementaties per OpenMRS-versie    | Duplicatie module-breed 1,9% geen aparte hotspot; zie `baseline-scope-export.txt` voor nLines per submodule |


---

## 7. Beperkingen en aannames


| #   | Beperking / aanname                                                     | Effect op analyse                                                                                                                                                                                                  |
| --- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 1   | SonarCloud **free plan**. Dus geen custom quality gates                 | Drempel duplication 3% i.p.v. NFR 5%; gedocumenteerd in §4.2                                                                                                                                                       |
| 2   | CI draait momenteel **zonder tests** in deploy-workflow (`-DskipTests`) | Coverage en test-resultaten vereisen lokale/`mvn test`-run of aparte CI-stap                                                                                                                                       |
| 3   | Baseline CC-cijfers OMOD handmatig uit SonarCloud-dashboard             | Export **2026-06-11**; filter `omod/`, regel `java:S3776`, drempel 15, zie ook `baseline-scope-export.txt`                                                                                                         |
| 4   | Module bevat meerdere `api-` submodules                                 | SonarCloud/module-breed scope: `api` (18.951 nLines), `api-tests` (12.901), `omod` (1.643), `api-1.9` (173), `api-1.10` (1.197), `api-2.0` (205), `api-2.2` (403), `release-tests` (71) — totaal **35.544** nLines |


---

## 8. Conclusie analyse

### Belangrijkste bevindingen

1. **Cognitive complexity** is het acute baseline-probleem: **97** violations module-breed, **5** in OMOD, alle boven drempel 15.
2. `HtmlFormEntryController.getFormEntrySession` (CC 52) is de beste PoC-kandidaat: expliciete technical debt, hoge impact, beperkte scope.
3. `FormEntrySession` is de strategische hotspot op langere termijn (~1228 LOC); niet negeren in baseline.
4. **Duplicatie** is laag (1,9% / 1,7% ✅); **code smells** hoog (1.230 / 59 ❌); **coverage** laag (16,4% / 1,9% ❌); **Quality Gate** Passed voor nieuwe code ✅ (§3).
5. **CI/SonarCloud-integratie** is aanwezig via `ci.yml` (NFR-M5/M6); voor/na-metrieken PoC zijn gedocumenteerd in [`07-validatie-voor-na.md`](07-validatie-voor-na.md).

### NFR-status baseline (acceptatiecriterium 1)


| Acceptatiecriterium                        | Status                                                              |
| ------------------------------------------ | ------------------------------------------------------------------- |
| Baseline-metrieken module-breed vastgelegd | ✅ §3 compleet (8 metrieken)                                         |
| Traceerbare hotspots met locatie           | ✅ OMOD CC gedocumenteerd                                            |
| PoC-focus onderbouwd                       | ✅ §5                                                                |


