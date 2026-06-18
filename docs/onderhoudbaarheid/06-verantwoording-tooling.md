# Verantwoording tooling - onderhoudbaarheid PoC

**Datum:** 2026-06-18  
**Auteur:** Floris Bogers  
**Scope:** Extract Method-refactor op `HtmlFormEntryController.getFormEntrySession` + CI/SonarCloud-integratie

Gerelateerde documenten: [`05-verantwoording-ai-tests.md`](../05-verantwoording-ai-tests.md) (characterization tests) - [`05-ontwerp-refactoring.md`](05-ontwerp-refactoring.md) (ontwerp) - [`07-validatie-voor-na.md`](../07-validatie-voor-na.md) (voor/na-metrieken)

---

## 1. Overzicht tooling per activiteit

| Activiteit | Tool / aanpak | Menselijke validatie | Status |
|------------|---------------|----------------------|--------|
| Characterization tests (T1-T9) | Cursor AI-agent - test-skeletons o.b.v. pad-tabel §7.3 | Review assertions + `mvn test` lokaal groen | gedocumenteerd in `05-verantwoording-ai-tests.md` |
| Extract Method-refactor `getFormEntrySession` | Cursor AI-agent (Composer) - suggesties voor extractie-grenzen en JavaDoc | Review methodgrenzen, naamgeving, gedragsconservering via T1-T9 als vangnet | zie §2 |
| Unit tests geextraheerde methoden (8 extra) | Cursor AI-agent - test-skeletons o.b.v. method-signaturen | Review assertions, `mvn -pl omod test verify` lokaal groen | zie §3 |
| CC-meting na refactor | Handmatig + SonarCloud CI | CI-rapport op PR | zie §4 |
| SonarCloud CI-integratie | Handmatig (`ci.yml`, `sonar-project.properties`) | Workflow-run in GitHub Actions | zie §4 |

---

## 2. PoC-refactor - Extract Method

### 2.1 Wat AI deed

Cursor AI (Composer-modus) is ingezet voor de Extract Method-refactor op `getFormEntrySession` (143 regels, CC 52). De AI:

- stelde extractie-grenzen voor op basis van de methode-inhoud
- genereerde method-signaturen en JavaDoc voor de nieuwe methoden (`resolveMode`, `resolveFormEntryContext`, `resolvePatientForSession`, `buildFormEntrySession`)
- maakte de inner class `FormEntryResolution` aan als value object voor de context-resolutie

### 2.2 Wat de mens deed

De mens (Floris Bogers) bepaalde en valideerde:

- **de scope van de PoC**: Extract Method i.p.v. de oorspronkelijk geplande Extract Class (`FormEntryRequestResolver`) - motivatie in [`05-ontwerp-refactoring.md`](05-ontwerp-refactoring.md) §2.3
- **de extractie-grenzen**: welke logica bij `resolveFormEntryContext` hoort vs. bij `buildFormEntrySession`
- **naamgeving**: `resolveFormEntryContext` in plaats van generiekere AI-suggesties
- **gedragsconservering**: alle 19 bestaande tests (T1-T9 + OMOD-suite) na de refactor lokaal groen (`mvn -pl omod test verify` - BUILD SUCCESS, 27 tests)
- **review van `FormEntryResolution`**: inner class i.p.v. top-level type; bewuste keuze om Spring-injectie-complexiteit te vermijden

### 2.3 Waarom AI hier geschikt was

De Extract Method-refactor is een **structuurwijziging zonder gedragswijziging**: de logica wordt verplaatst, niet herschreven. Dit is een repetitieve, goed-specificeerbare taak waarbij AI snel skelet-code levert. De correctheid wordt gewaarborgd door de bestaande characterization tests als vangnet - het principe van "add tests before you change code" (Fowler) dat in de teststrategie is vastgelegd.

### 2.4 Beperkingen

- AI kent de OpenMRS API (`HtmlFormEntryUtil`, `EncounterServiceCompatibility`) niet standaard; method-bodies moesten worden gecontroleerd op correcte service-aanroepen.
- De `which`-logica (`first`/`last` encounter) is subtiel: AI plaatste die initieel buiten `resolveFormEntryContext`; handmatig teruggeplaatst.
- `buildFormEntrySession` bevat concurrency-checks op timestamps - foutgevoelig bij automatische refactor; handmatig geverifieerd via T8/T9.

---

## 3. Unit tests geextraheerde methoden

Na de refactor zijn 8 aanvullende tests geschreven voor de geextraheerde methoden (naast de bestaande T1-T9 characterization tests).

### 3.1 Wat AI deed

Cursor AI genereerde test-skeletons op basis van de method-signaturen van `resolveMode`, `resolveEncounterById`, `resolveHtmlFormForEncounter`, `resolvePatient`, `resolveHtmlForm`, `resolvePatientForSession`.

### 3.2 Wat de mens deed

- review van elke assertion: klopt het verwachte gedrag met de broncode?
- fixture-keuzes: welke OpenMRS-testdata volstaat per test
- `mvn -pl omod test verify` lokaal uitgevoerd: 27/27 groen (BUILD SUCCESS)

### 3.3 Resultaat

| Metriek | Waarde |
|---------|--------|
| Totaal tests na PoC | 27 (was 19) |
| Nieuwe tests | 8 (geextraheerde methoden) |
| Regressie | 0 - alle T1-T9 nog groen |
| JaCoCo baseline na characterization | ~53% op `HtmlFormEntryController` |

---

## 4. SonarCloud en CI

De SonarCloud-integratie is handmatig opgezet zonder AI.

| Component | Aanpak | Bewijs |
|-----------|--------|--------|
| `ci.yml` - `sonarcloud`-job | Handmatig geschreven; `sonar.qualitygate.wait=true` | `.github/workflows/ci.yml` |
| `sonar-project.properties` | Handmatig geconfigureerd | `sonar-project.properties` in repo-root |
| `SONAR_TOKEN` | GitHub Secret - handmatig ingesteld | - |
| Quality Gate-status op PoC-PR | Afhankelijk van CI-run - open punt in `07-validatie-voor-na.md` §3.5 | CI-run URL toevoegen na PR |

**Waarom geen AI voor CI-setup:** CI-configuratie heeft directe invloed op de kwaliteitsborging van de pipeline (NFR-M5/M6). Fouten in `ci.yml` kunnen de Quality Gate omzeilen; handmatige controle is hier vereist.

---

## 5. Kritische reflectie

### Wat werkte goed

AI versnelde de repetitieve structuurwijzigingen significant: de Extract Method-refactor en de bijbehorende test-skeletons waren samen in een fractie van de tijd klaar die een volledig handmatige aanpak zou kosten. De combinatie van AI-snelheid en menselijke review via bestaande tests als vangnet werkte effectief.

### Wat minder goed werkte

- **Domeinkennis:** AI heeft geen kennis van de OpenMRS module-testinfrastructuur (`BaseModuleContextSensitiveTest`, Hibernate-mapping, `TestingApplicationContext.xml`). Setup-fouten werden pas zichtbaar bij `mvn test` en vereisten handmatige correctie.
- **Subtiele logica:** De `which`-logica en timestamp-concurrency-checks zijn door AI initieel niet correct geplaatst. Dit onderstreept dat AI geen vervanger is voor domeinbegrip.
- **Naamgeving:** AI-suggesties voor methodenamen waren generiek; menselijke review was nodig voor namen die de intentie goed uitdrukken.

### Conclusie

AI is ingezet als productiviteitsinstrument voor goed-specificeerbare, repetitieve taken (structuurwijzigingen, test-skeletons, JavaDoc). De eindverantwoordelijkheid - correctheid, architecturale keuzes, gedragsconservering - lag bij de mens. Dit past bij de projectkeuze zoals beschreven in [`05-verantwoording-ai-tests.md`](../05-verantwoording-ai-tests.md).

---

## 6. Traceerbaarheid

| Activiteit | Commit / bewijs |
|------------|----------------|
| Extract Method-refactor | Commit `4672154`, `3caea0c` op `refactor/htmlformentry-controller-poc` |
| 8 extra unit tests | `HtmlFormEntryControllerTest` - 18 @Test-methoden totaal |
| 27/27 tests groen | `mvn -pl omd test verify` lokaal - BUILD SUCCESS |
| SonarCloud CI-setup | `.github/workflows/ci.yml`, `sonar-project.properties` |
