# Security backlog - bevindingenregister

**Module:** OpenMRS HTML Form Entry v3.10.0
**Datum:** 2026-06-11
**Beoordelaar:** frogissober
**Bron:** Snyk SCA-scan (`snyk test --all-projects`, CI-artifact `snyk-results.json`) + handmatige code-analyse
**Scope:** Productie-afhankelijkheden (Maven) en gebundelde front-end-libraries van de module

---

## 6.1 Aanpak

De SCA-scan uit de CI-pipeline vond over alle Maven-submodules samen **223 unieke kwetsbaarheden**: 12 critical, 130 high, 68 medium en 13 low. Dit register bevat een selectie daarvan. We hebben alle critical-bevindingen meegenomen die in deze module ook echt te misbruiken zijn. Daarnaast hebben we zelf de gebundelde JavaScript-libraries onderzocht. Die ziet de Maven-scan namelijk niet. Dat laat meteen zien dat je niet blind op de scanner kunt vertrouwen.

**Contextuele score:** de CVSS-score zegt hoe erg een kwetsbaarheid in het algemeen is, los van de omgeving. De contextuele score kijkt naar twee dingen: kan een aanvaller er in deze module echt bij, en welk kroonjuweel uit hoofdstuk 3 wordt geraakt (BIV-classificatie). Een critical CVE in een component die nooit wordt aangeroepen scoort dus lager. Een medium CVE in het formulier-invoerpad scoort juist hoger, omdat daar de assets met C=5 zitten.

**Dependency-context:** bijna alle Maven-bevindingen komen transitief binnen via `org.openmrs.api:openmrs-api@1.9.9` en `org.openmrs.web:openmrs-web@1.9.9` (scope `provided`). Het OpenMRS-platform levert deze libraries tijdens runtime; de module bundelt ze niet zelf. Hoe groot het risico echt is hangt dus af van de platformversie waarop de module draait. De scan tegen de minimaal ondersteunde versie (1.9.9) is het worst-case scenario. De gebundelde JavaScript-libraries zitten wél in het eigen omod-bestand. Daar is de module dus zelf verantwoordelijk voor.

## 6.2 Legenda

| Veld | Betekenis |
|------|-----------|
| Prioriteit | H = direct oppakken (Sprint 3-kandidaat) · M = inplannen · L = accepteren of monitoren |
| Status | Open · In behandeling · False positive |
| Contextuele score | CVSS bijgesteld op bereikbaarheid van het aanvalspad + geraakt kroonjuweel (h3) |

## 6.3 Backlog

| Finding ID | Component | CVE / CWE | CVSS | Context. score | NEN-7510:2024-2 control | Prioriteit | Status | False positive-rationale |
|------------|-----------|-----------|------|----------------|-------------------------|------------|--------|--------------------------|
| HFE-001 | log4j 1.2.15 (via openmrs-api) | CVE-2019-17571 / CWE-502 | 9.8 | 6.5 | 8.8 · 8.15 | **H** | Open | n.v.t. |
| HFE-002 | commons-fileupload 1.2.1 (via openmrs-web) | CVE-2016-1000031 / CWE-284 | 9.8 | 8.5 | 8.8 | **H** | Open | n.v.t. |
| HFE-003 | jQuery 1.8.3 + jQuery UI 1.9.2 (gebundeld) | CVE-2020-11023, CVE-2021-41184 / CWE-79 | 6.1 | 8.0 | 8.28 | **H** | Open | n.v.t. |
| HFE-004 | spring-beans 3.0.5.RELEASE (via openmrs-api) | CVE-2022-22965 / CWE-94 | 9.8 | 7.0 | 8.8 | **H** | Open | n.v.t. |
| HFE-005 | commons-collections 3.2 (via openmrs-api) | CVE-2015-7501 / CWE-502 | 9.8 | 6.5 | 8.8 | M | Open | n.v.t. |
| HFE-006 | groovy 1.8.3 (directe dependency, `api/pom.xml:55`) | CVE-2015-3253 / CWE-74 | 9.8 | 5.5 | 8.8 · 8.28 | M | Open | n.v.t. |
| HFE-007 | handlebars 1.0.12 (gebundeld) | CVE-2019-19919 / CWE-1321 | 9.8 | 5.5 | 8.28 | M | Open | n.v.t. |
| HFE-008 | commons-codec 1.10 (via openmrs-test → dbunit) | SNYK-JAVA-COMMONSCODEC-561518 / CWE-200 | 3.7 | - | 8.8 | L | **False positive** | Komt alleen via `openmrs-test`/`dbunit` in de test-classpath. Zit niet in het productie-omod-bestand en is in productie dus niet bereikbaar. |

## 6.4 Toelichting per bevinding

**HFE-001 - log4j 1.2.15 (end-of-life).** Log4j 1.x wordt sinds 2015 niet meer onderhouden, dus er komen geen patches meer (`fixedIn: []` in de scan-output). Er staan zeven Snyk-issues open op deze component. De score voor CVE-2019-17571 (deserialisatie via `SocketServer`) hebben we verlaagd: de module gebruikt in `api/src/main/resources/log4j.xml` alleen een `ConsoleAppender`, dus deze aanval werkt hier in de praktijk niet. De prioriteit blijft wel **H**. Logging is het bewijsmiddel voor control 8.15 (zie audit-logbestanden in h3, I=5), en een component zonder patches is sowieso een probleem onder control 8.8. Raakt kroonjuweel: audit-logbestanden.

**HFE-002 - commons-fileupload 1.2.1.** Via CVE-2016-1000031 kan een aanvaller code uitvoeren door een `DiskFileItem` te deserialiseren. We hebben de score juist verhoogd: de module biedt zelf bestandsupload aan via `UploadWidget.java`, dus dit code-pad wordt echt gebruikt. Er staan nog zeven andere issues open op deze component (o.a. CVE-2025-48976). Een fix bestaat vanaf versie 1.3.3, maar het platform bepaalt welke versie er draait. Het patchadvies (h7) adviseert daarom op de minimaal vereiste platformversie. Raakt: klinische obs-data en zorgprocescontinuïteit.

**HFE-003 - jQuery 1.8.3 + jQuery UI 1.9.2 (gebundeld).** De module bundelt en laadt deze libraries zelf (`omod/src/main/webapp/resources/`, geladen in o.a. `htmlFormEntry.jsp:22-26`). jQuery 1.8.3 bevat meerdere DOM-XSS-kwetsbaarheden (o.a. CVE-2020-11023), jQuery UI 1.9.2 bevat CVE-2021-41184. De CVSS-score is maar 6.1, maar wij scoren dit op 8.0. Dit is een formulierapplicatie waarin zorgverleners vrije invoer verwerken. Met een XSS kan een aanvaller de sessie van een ingelogde zorgverlener overnemen, en daarmee bij de kroonjuwelen met C=5 (klinische obs-data, patiëntidentiteit). Anders dan bij de Maven-bevindingen is de module hier zelf verantwoordelijk. Sterke kandidaat voor Sprint 3 (taak 3.1): de libraries upgraden kan binnen de module zelf.

**HFE-004 - spring-beans 3.0.5 ("Spring4Shell").** CVE-2022-22965 (RCE via data binding) raakt 7 van de submodules. We hebben de score iets verlaagd, want de exploit werkt alleen onder specifieke omstandigheden (JDK 9+, WAR-deployment op Tomcat). De module gebruikt Spring MVC-databinding wel volop in haar controllers (`omod/src/main/java/.../web/controller/`). Het kwetsbare mechanisme wordt dus actief gebruikt. Daarom 7.0 en prioriteit **H**.

**HFE-005 - commons-collections 3.2.** Klassieke deserialisatie-gadget-chain (CVE-2015-7501). Zo'n gadget-chain werkt alleen als er ergens onvertrouwde data wordt gedeserialiseerd. De module deserialiseert formulierdata via XStream (`SerializableFormObject.java:177`, via de OpenMRS `SerializationService`). We kunnen dus niet uitsluiten dat dit pad bestaat. Contextueel 6.5, prioriteit M omdat de invoer via ingelogde sessies loopt. Fix beschikbaar: 3.2.2.

**HFE-006 - groovy 1.8.3 (directe dependency).** De enige critical die de module zelf declareert. Het commentaar in `api/pom.xml:53` zegt "required only to run test scripts (scope=test)", maar de `<scope>test</scope>`-tag ontbreekt. Daardoor zit de dependency gewoon in de compile-classpath en dus in de productiemodules. CVE-2015-3253 werkt alleen als onvertrouwde invoer bij een Groovy-evaluatie komt. Daar hebben we geen direct pad voor gevonden, vandaar 5.5. De fix is simpel en leerzaam: de scope expliciet op `test` zetten. Kandidaat voor Sprint 3.

**HFE-007 - handlebars 1.0.12 (gebundeld).** Prototype pollution (CVE-2019-19919, gefixt in 4.3.0). Score verlaagd: de Handlebars-templates komen uit de eigen module-code, niet uit gebruikersinvoer. Template-injectie heeft hier dus geen direct pad. Wel meenemen in dezelfde upgrade-ronde als HFE-003, want het zit in dezelfde bestanden (`htmlForm.jsp`, `htmlFormEntry.jsp`).

**HFE-008 - commons-codec 1.10 (false positive).** De scanner meldt dit wel, maar het pad loopt via `org.openmrs.test:openmrs-test@2.2.0 → dbunit@2.5.4`. Dat is een pure test-dependency. De component zit niet in het productie-omod-bestand en is in productie dus niet bereikbaar. We laten de bevinding bewust in het register staan met deze onderbouwing, in plaats van hem stil te verwijderen. Zo is het beoordelingsproces controleerbaar (control 8.8). *Let op:* de root-`pom.xml` declareert zelf ook `commons-codec:1.10` met scope `provided`. Bij een platform-upgrade verdwijnt deze bevinding vanzelf.

## 6.5 Vervolg

De H-prioriteiten uit dit register zijn de input voor het patchadvies (h7) en voor de keuze van de PoC-mitigaties in Sprint 3 (taak 3.1). HFE-003 en HFE-006 zijn daarvoor de beste kandidaten. Beide zijn binnen de module zelf op te lossen, zonder dat er een platform-upgrade nodig is. En voor beide kun je met een hertest aantonen dat de fix werkt (taak 3.2/3.3).
