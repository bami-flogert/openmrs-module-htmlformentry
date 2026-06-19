# Verantwoording tooling — onderhoudbaarheid PoC

**Datum:** 18 juni 2026 · **Branch:** `acceptatie`  
**Auteur:** Floris Bogers (PoC/AI) + pipeline-bewijs (Teamlid A)

Gerelateerde documenten: [`05-verantwoording-ai-tests.md`](../05-verantwoording-ai-tests.md) (characterization tests) · [`05-ontwerp-refactoring.md`](05-ontwerp-refactoring.md) (ontwerp) · [`07-validatie-voor-na.md`](../07-validatie-voor-na.md) (voor/na-metrieken) · [`04-testresultaten-baseline.md`](../04-testresultaten-baseline.md) (CI/Sonar-handoff)

| Fase                          | Document / tooling                                                  | Status |
| ----------------------------- | ------------------------------------------------------------------- | ------ |
| Characterization tests (AI)   | [`05-verantwoording-ai-tests.md`](../05-verantwoording-ai-tests.md) | ✅      |
| PoC-refactor (Extract Method) | `HtmlFormEntryController.getFormEntrySession`                       | ✅      |
| SonarCloud CI / quality gate  | [`sonarcloud-setup.md`](../sonarcloud-setup.md)                     | ✅      |
| Test- en CI-bewijs            | [`04-testresultaten-baseline.md`](../04-testresultaten-baseline.md) | ✅      |
| Validatie voor/na             | [`07-validatie-voor-na.md`](../07-validatie-voor-na.md)             | ✅      |

---

## 1. Overzicht tooling per activiteit

| Activiteit | Tool / aanpak | Menselijke validatie | Status |
|------------|---------------|----------------------|--------|
| Characterization tests (T1–T9) | Cursor AI-agent — test-skeletons o.b.v. pad-tabel §7.3 | Review assertions + `mvn test` lokaal groen | [`05-verantwoording-ai-tests.md`](../05-verantwoording-ai-tests.md) |
| Extract Method-refactor `getFormEntrySession` | Cursor AI-agent (Composer) — extractie-grenzen en JavaDoc | Review methodgrenzen, naamgeving, T1–T9 als vangnet | §2 |
| Unit tests geëxtraheerde methoden | Cursor AI-agent — test-skeletons o.b.v. method-signaturen | Review assertions, `mvn -pl omod test verify` groen | §3 |
| CC-meting na refactor | Handmatig + SonarCloud CI | CI-rapport PR #31/#37 | §4 |
| SonarCloud CI-integratie | Handmatig (`ci.yml`, `sonar-project.properties`) | Workflow-run GitHub Actions | §4 |

**Gepland:** Extract Class → `FormEntryRequestResolver`.  
**Uitgevoerd:** Extract Method + Move Method binnen `HtmlFormEntryController` en `FormEntrySession`.

| Hulpmiddel             | Rol                                                                                                        |
| ---------------------- | ---------------------------------------------------------------------------------------------------------- |
| IDE-refactoring        | Extract Method (`resolveMode`, `resolveFormEntryContext`, `buildFormEntrySession`, …)                      |
| Characterization tests | Regressievangnet vóór en na refactor ([`05-verantwoording-ai-tests.md`](../05-verantwoording-ai-tests.md)) |
| SonarCloud             | Validatie CC (S3776) en new-code smells                                                                    |
| JaCoCo                 | Coverage-bewijs op PoC-hotspot                                                                             |

**Resultaat:** `getFormEntrySession` CC **52 → geen S3776-violation** op changed code. Coverage `HtmlFormEntryController` **2,1% → ~63%** (JaCoCo).

---

## 2. PoC-refactor — Extract Method

### 2.1 Wat AI deed

Cursor AI (Composer-modus) is ingezet voor de Extract Method-refactor op `getFormEntrySession` (143 regels, CC 52). De AI:

- stelde extractie-grenzen voor op basis van de methode-inhoud
- genereerde method-signaturen en JavaDoc voor de nieuwe methoden (`resolveMode`, `resolveFormEntryContext`, `resolvePatientForSession`, `buildFormEntrySession`)
- maakte de inner class `FormEntryResolution` aan als value object voor de context-resolutie

### 2.2 Wat de mens deed

De mens (Floris Bogers) bepaalde en valideerde:

- **de scope van de PoC**: Extract Method i.p.v. de oorspronkelijk geplande Extract Class (`FormEntryRequestResolver`) — motivatie in [`05-ontwerp-refactoring.md`](05-ontwerp-refactoring.md) §2.3
- **de extractie-grenzen**: welke logica bij `resolveFormEntryContext` hoort vs. bij `buildFormEntrySession`
- **naamgeving**: `resolveFormEntryContext` in plaats van generiekere AI-suggesties
- **gedragsconservering**: alle characterization-tests (T1–T9) en volledige OMOD-suite na refactor groen (`mvn -pl omod test verify` — **67 tests**, BUILD SUCCESS)
- **review van `FormEntryResolution`**: inner class i.p.v. top-level type; bewuste keuze om Spring-injectie-complexiteit te vermijden

### 2.3 Waarom AI hier geschikt was

De Extract Method-refactor is een **structuurwijziging zonder gedragswijziging**: de logica wordt verplaatst, niet herschreven. Correctheid wordt gewaarborgd door characterization tests als vangnet — "add tests before you change code" (Fowler), vastgelegd in [`03-teststrategie.md`](../03-teststrategie.md).

### 2.4 Beperkingen

- AI kent de OpenMRS API (`HtmlFormEntryUtil`, `EncounterServiceCompatibility`) niet standaard; method-bodies moesten worden gecontroleerd op correcte service-aanroepen.
- De `which`-logica (`first`/`last` encounter) is subtiel: AI plaatste die initieel buiten `resolveFormEntryContext`; handmatig teruggeplaatst.
- `buildFormEntrySession` bevat concurrency-checks op timestamps — handmatig geverifieerd via T8/T9.

---

## 3. Unit tests geëxtraheerde methoden

Na de refactor zijn extra tests geschreven voor de geëxtraheerde methoden (naast T1–T9 characterization tests in `HtmlFormEntryControllerTest` en dedicated tests in `HtmlFormEntryControllerExtractedMethodsTest`).

### 3.1 Wat AI deed

Cursor AI genereerde test-skeletons op basis van de method-signaturen van `resolveMode`, `resolveEncounterById`, `resolveHtmlFormForEncounter`, `resolvePatient`, `resolveHtmlForm`, `resolvePatientForSession`.

### 3.2 Wat de mens deed

- review van elke assertion: klopt het verwachte gedrag met de broncode?
- fixture-keuzes: welke OpenMRS-testdata volstaat per test
- `mvn -pl omod test verify` lokaal uitgevoerd: **67/67 groen** (BUILD SUCCESS)

### 3.3 Resultaat

| Metriek | Waarde |
|---------|--------|
| Totaal omod-tests na PoC | **67** |
| `HtmlFormEntryControllerTest` (T1–T9) | 13 |
| `HtmlFormEntryControllerExtractedMethodsTest` | 18 |
| Regressie | 0 — alle T1–T9 nog groen |
| JaCoCo `HtmlFormEntryController` | **~63%** (99/157 lines) |

Zie [`04-testresultaten-baseline.md`](../04-testresultaten-baseline.md) § Scope A en [`07-validatie-voor-na.md`](../07-validatie-voor-na.md) §2.

---

## 4. SonarCloud en CI

De SonarCloud-integratie is handmatig opgezet (geen AI). Zie [`sonarcloud-setup.md`](../sonarcloud-setup.md): `SONAR_TOKEN`, CI-based analysis, branch protection met verplichte check **SonarCloud Analysis**.

### `sonar.qualitygate.wait=true`

In [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) wacht de `sonarcloud`-job op de Quality Gate vóór de job als geslaagd telt. Bij **FAILED** faalt de pipeline — bewuste merge-rem (NFR-M5/M6).

**Bewijs PR #31:** [CI-run 27682116331](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27682116331) — `QUALITY GATE STATUS: PASSED`.

### Metrieken na refactor (PR #31)

| Metriek                | Waarde  |
| ---------------------- | ------- |
| New S3776 (CC)         | 0       |
| New code smells        | 5 Major |
| Maintainability rating | A       |
| Quality Gate           | Passed  |

### JaCoCo-import in Sonar-scan (opgelost)

**Oorzaak:** `sonar.coverage.jacoco.xmlReportPaths` in parent `pom.xml` gebruikte repo-root-paden (`omod/target/...`). Sonar resolveert die per module vanuit de module-directory, waardoor het rapport niet gevonden werd (*No report imported*).

**Fix:** property verwijderd uit `pom.xml` en `sonar-project.properties`. Sonar gebruikt nu het standaardpad `target/site/jacoco/jacoco.xml` per module (`api`, `omod`). CI-stap **Verify JaCoCo reports exist** in `ci.yml` bevestigt aanwezigheid vóór `sonar:sonar`.

**Verificatie PR #37:** [CI-run 27749957935](https://github.com/bami-flogert/openmrs-module-htmlformentry/actions/runs/27749957935) — JaCoCo verify `OK` voor `api` + `omod`; Sonar-log: `QUALITY GATE STATUS: PASSED`; dashboard [PR #37](https://sonarcloud.io/dashboard?id=bami-flogert_openmrs-module-htmlformentry&pullRequest=37).

**Waarom geen AI voor CI-setup:** CI-configuratie heeft directe invloed op kwaliteitsborging (NFR-M5/M6). Fouten in `ci.yml` kunnen de Quality Gate omzeilen; handmatige controle is hier vereist.

### Nieuwe smells — trade-off

Vijf Major smells op changed code, geen blockers:

1. **FormEntrySession** (4×): geneste `if`s en `RuntimeException` in `validateNotModifiedSinceTimestamps` — logisch gevolg van Move Method uit de controller.
2. **HtmlFormEntryController** (1×): `buildFormEntrySession` met 9 parameters — gevolg van Extract Method-orchestratie.

Geen gedragsregressie: **67 omod-tests + regressie-subset 58/58 groen**. Opruimen is optioneel; niet nodig voor Quality Gate.

---

## 5. Kritische reflectie

| Aspect | Beoordeling |
|--------|-------------|
| **SonarCloud** | Nuttig voor CC-detectie (S3776) en merge-gate; JaCoCo-import via standaardpad per module (fix juni 2026) |
| **AI (tests + refactor)** | Characterization tests maakten Extract Method veilig; AI versnelde repetitieve structuurwijzigingen — zie [`05-verantwoording-ai-tests.md`](../05-verantwoording-ai-tests.md) |
| **Ontwerp vs implementatie** | Extract Method i.p.v. Extract Class — sneller, minder bestanden; 5 new smells en controller nog ~567 LOC zijn acceptabele trade-offs ([`05-ontwerp-refactoring.md`](05-ontwerp-refactoring.md)) |

### Wat minder goed werkte

- **Domeinkennis:** AI heeft geen kennis van OpenMRS module-testinfrastructuur (`BaseModuleContextSensitiveTest`, Hibernate-mapping). Setup-fouten pas zichtbaar bij `mvn test`.
- **Subtiele logica:** `which`-logica en timestamp-concurrency-checks initieel niet correct geplaatst door AI.
- **Naamgeving:** AI-suggesties generiek; menselijke review nodig voor intentie-uitdrukkende namen.

**Conclusie:** AI als productiviteitsinstrument voor goed-specificeerbare taken; eindverantwoordelijkheid (correctheid, architectuur, gedragsconservering) bij de mens.

---

## 6. Traceerbaarheid

| Activiteit | Bewijs |
|------------|--------|
| Extract Method-refactor | PR #31 merge naar `acceptatie` |
| Unit tests geëxtraheerde methoden | `HtmlFormEntryControllerExtractedMethodsTest` (18 tests) |
| 67/67 omod-tests groen | [`04-testresultaten-baseline.md`](../04-testresultaten-baseline.md) § Scope A |
| Regressie-subset api-tests | 58/58 groen — [`07-validatie-voor-na.md`](../07-validatie-voor-na.md) §2.2 |
| SonarCloud CI-setup | `.github/workflows/ci.yml`, `sonar-project.properties` |
