# Verantwoording (AI-)tooling — security

**Module:** OpenMRS HTML Form Entry v3.10.0  
**Datum:** 18 juni 2026  
**Status:** Afgerond

> Doel van dit document: per gebruikt hulpmiddel verantwoorden **wat** het is, **waarvoor**
> het in dit security-onderzoek is ingezet, en **kritisch reflecteren** op de waarde en de
> grenzen ervan. Dit dekt het rubriekonderdeel *"realisatie verantwoord, inclusief gebruik
> van (AI)tooling"* en de "Goed"-eis van een kritische reflectie.

---

## 1. AI-tooling

### 1.1 Governance AI-gebruik (teamafspraak)

- AI-tools ondersteunden development, documentatie en reviews; zij namen **geen besluiten** en mergeerden niet zelf.
- Elke PR met AI-bijdrage (code of Copilot-review) werd door **minimaal één groepslid** gecontroleerd vóór merge.
- Copilot-review-suggesties werden geaccepteerd, aangepast of gemotiveerd genegeerd.
- Strategische keuzes (scope, prioritering, security-score) werden in **teamoverleg** genomen; AI diende als sparring, niet als autoriteit.
- Er is geen volledige audittrail van alle prompts; validatie gebeurde via review, tests en reproduceerbare commando's (bijv. `mvn -pl omod test verify`, pentest hertests).

### 1.2 Tooling per fase

| Tool | Wat het is | Waarvoor gebruikt | Kritische reflectie |
|------|-----------|-------------------|---------------------|
| **Cursor AI (Composer 2.5)** | AI-agent in de IDE | Development: characterization tests (`HtmlFormEntryControllerTest` T1–T9), workflow-YAML, audit- en pentest-documentatie, mitigatiecode | Versnelde repetitieve taken (pad-tabel → JUnit). Kende OpenMRS module-testcontext niet standaard; mens voegde `TestingApplicationContext.xml`, `simplestForm.xml` en fixture-keuzes toe. Validatie: `mvn -pl omod test verify` → 34/34 groen. Zie [`05-verantwoording-ai-tests.md`](../05-verantwoording-ai-tests.md). |
| **Claude Code** | AI-codeassistent in terminal/IDE | Development: code-analyse, refactor-voorstellen, gap-analyse en security-documentatie | Zelfde governance als Cursor. Sterk bij structureren van lange documenten en het uitschrijven van traceability; minder betrouwbaar op OpenMRS-specifieke runtime-gedragingen zonder menselijke test-run. |
| **GitHub Copilot** | AI code-completion en PR review-suggesties op GitHub | PR reviews: suggesties op pipeline-wijzigingen, dependency-config en documentatie | Altijd door groepslid gecontroleerd. Voorbeeld bijsturing: suggestie om volledige reactor-tests in CI te draaien → team scoped naar `omod` i.v.m. bekende baseline-failures ([`review-pr15.md`](../review-pr15.md) §CI). |
| **Gemini / Claude (discussie)** | AI als sparringpartner (per teamlid) | Teamoverleg: scope-afbakening (PoC vs. volledige refactor), prioritering backlog, interpretatie NEN-controls | Geen directe code-merge. Waarde: sneller opties vergelijken; risico: overconfidence bij security-scores — daarom altijd gekoppeld aan menselijke besluitvorming en scanner-/pentest-bewijs. |

---

## 2. Security-scanning & SAST

| Tool | Wat het is | Waarvoor gebruikt | Kritische reflectie |
|------|-----------|-------------------|---------------------|
| **Snyk (SCA)** | Dependency-/kwetsbaarheidsscan (`snyk test --all-projects`) | Inventarisatie van 223 unieke Maven-kwetsbaarheden; input voor security backlog (HFE-001 t/m HFE-008) en patchadvies | Sterk voor transitieve Java-deps. **Blinde vlek:** gebundelde front-end-libraries (jQuery 1.8.3, jQuery UI 1.9.2) niet gezien → handmatig aangevuld in [`06-security-backlog.md`](06-security-backlog.md). Platform-deps (`provided`) zijn worst-case; module patcht die niet zelf. Soft gate: `continue-on-error: true`; vereist `SNYK_TOKEN`. |
| **Snyk Code (SAST)** | Statische code-analyse op Java-broncode | Aanvullende SAST naast handmatige gap-analyse; CI-artifact `snyk-code-results.json` | Vond geen van de drie pentest-kwetsbaarheden (path traversal, CSRF/IDOR, open redirect) — bevestigt dat SAST alleen niet volstaat. False positives mogelijk; menselijke triage nodig. |
| **zizmor** | SAST voor GitHub Actions-workflows en Dependabot | Pipeline-security: baseline 58 bevindingen (43 high); remediatie template-injection, artipacked, dependabot-cooldown | Unieke meerwaarde t.o.v. Snyk/SonarCloud: scant CI/CD-config zelf. 40 high open (vooral `unpinned-uses`, `cache-poisoning`) — bewust niet allemaal opgelost; CI-job draait met `continue-on-error: true`. Zie [`zizmor.md`](../zizmor.md). |
| **Dependabot** | Geautomatiseerde dependency-updates (Maven/Actions/Docker) | Wekelijkse updates; cooldown 7 dagen tegen update-storm | Platform-deps (`openmrs-api`, `openmrs-web`) bewust genegeerd — module kan die niet zelf upgraden. Dependabot vervangt geen security-beoordeling: update ≠ veilig zonder regressietest. |
| **Dependency review** | PR-check op nieuwe kwetsbare dependencies | Blokkeert (of waarschuwt bij) nieuwe kwetsbare deps op pull requests in `ci.yml` | Alleen actief bij dependency-wijzigingen in PRs; geen dekking van gebundelde JS of runtime-platform. Aanvulling op Snyk, geen vervanging. |
| **SonarCloud** | SAST/kwaliteitsanalyse (Java) | Cognitive complexity, smells, security hotspots; quality gate op nieuwe code in CI | Nuttig voor prioritering (CC 52 op hotspot), niet voor exploitatie-paden. Geen vervanging van pentest; token-afhankelijk (`SONAR_TOKEN`). Zie [`sonarcloud-setup.md`](../sonarcloud-setup.md). |

---

## 3. SBOM & supply chain

| Tool | Wat het is | Waarvoor gebruikt | Kritische reflectie |
|------|-----------|-------------------|---------------------|
| **GitHub Dependency Graph / SPDX** | SBOM-generatie via GitHub API in `deploy.yml` | SPDX SBOM (`docs/sbom.spdx.json`) per build; gebundeld in `otap-build-bundle` en prod release | Machineleesbaar auditspoor (NEN A.8.15). Alleen wat GitHub in de dependency graph ziet — gebundelde JS in `omod/src/main/webapp/resources/` ontbreekt in SPDX. |
| **CycloneDX (`snyk sbom`)** | Machineleesbare SBOM via Snyk | Aanvullend SBOM-formaat in `snyk.yml`; input naast SPDX voor patchadvies | Token-afhankelijk; parallel aan SPDX voor CRA/supply-chain-context. Twee formaten = meer werk, maar betere interoperabiliteit. |
| **`generate-patch-advice.py`** | Eigen Python-script: Snyk JSON + SBOM → geprioriteerd patchplan | Genereert [`07-patchadvies.md`](07-patchadvies.md): patchvolgorde op CVSS, Docker-image-adviezen, verwachte risicoreductie | **Zelfbouw:** reproduceerbaar (`python3 .github/scripts/generate-patch-advice.py`), maar onderhoudslast bij Snyk-outputwijzigingen. Voordeel: project-specifieke prioritering i.p.v. generieke Snyk-lijst. Mens valideerde output tegen backlog. |

---

## 4. Penetratietest-lab

| Tool | Wat het is | Waarvoor gebruikt | Kritische reflectie |
|------|-----------|-------------------|---------------------|
| **Docker / Docker Compose** | Geïsoleerd pentest-lab (`Dockerfile.pentest`, `docker-compose.pentest.yml`) | Reproduceerbare omgeving: OpenMRS Ref. App 2.12.2 + module 3.10.0; attack surface mapping en exploitatie HFE-01/02/03 | Lab ≠ productie (ephemeral runners, vaste images). Wel essentieel om SAST-bevindingen te **valideren** met echte HTTP-aanvallen. Opstarttijd en image-versies beperken frequentie van hertests. |
| **curl** | CLI voor HTTP-requests | Reproduceerbare aanvallen (path traversal, CSRF-PoC, open redirect) en hertests na mitigatie; bewijs in [`../pentest/bewijs/`](../pentest/bewijs/) | Laagdrempelig en scriptbaar (`smoke-test.sh`-patroon). Vereist handmatige interpretatie van responses; geen automatische regressie-suite voor pentests. |
| **Maven (build gepatchte module)** | Bouwt broncode → `.omod` voor lab | Gepatchte module na mitigatie (HFE-01/02/03) opnieuw gebouwd en in Docker getest | Build-reproduceerbaarheid gekoppeld aan JDK 8 + vaste commit. Pentest valideert **gedrag**, niet de hele dependency tree. |

**Resultaat pentest t.o.v. scanners:** drie kwetsbaarheden (arbitrary file read, CSRF/IDOR, open redirect) zijn met het lab aangetoond, in code gemitigeerd en hertest — zie [`10-traceability-matrix.md`](10-traceability-matrix.md) §1. Geen van deze drie kwam uit Snyk Code of Snyk SCA alleen.

---

## 5. Pipeline & overige

| Tool | Wat het is | Waarvoor gebruikt | Kritische reflectie |
|------|-----------|-------------------|---------------------|
| **GitHub Actions (CI/CD)** | OTAP-pipeline (`ci.yml`, `deploy.yml`, `snyk.yml`) | Vier OTAP-fasen, PR-checks, unit tests (PoC-scope `omod`), SBOM, deploy, smoke test, SonarCloud, zizmor | Sterk auditspoor (artifacts, logs). Deploy op ephemeral runners — geen persistente OTAP-servers ([`02-pipeline-compliance.md`](02-pipeline-compliance.md)). Secrets via GitHub Environments; branch protection moet in UI worden afgedwongen. |
| **`smoke-test.sh`** | Post-deploy curl-loop | Valideert dat OpenMRS reageert na deploy (`/openmrs`) | Detecteert uitval, geen security-regressie. Configureerbare poort (80 vs 8080) voorkomt false negatives in acceptatie. |
| **Handmatige code-review** | Menselijke analyse op `bestand:regel` | Gap-analyse NEN-7510 A.8.3/A.8.5/A.8.15 ([`01-gap-analyse.md`](01-gap-analyse.md)); attack surface mapping ([`../pentest/00-attack-surface-mapping.md`](../pentest/00-attack-surface-mapping.md)) | Meest betrouwbare bron voor modulespecifieke logica (authz-gaps, logging-PII). Arbeidsintensief; niet schaalbaar op 35k LOC — daarom combinatie met scanners + gerichte pentest. |

---

## 6. Samenvattende reflectie

De tooling in dit security-onderzoek vulde elkaar aan; geen enkele tool dekte het volledige plaatje.

**Meeste waarde**

- **Snyk SCA** voor een eerste overzicht van de dependency-risico's en input voor patchprioritering.
- **zizmor** voor pipeline-blind spots die Java-scanners niet zien (template-injection, credential-persist in checkout).
- **Docker-pentest-lab + curl** om drie concrete kwetsbaarheden aan te tonen en mitigaties te valideren — sterker bewijs dan een scanner-rapport alleen.
- **AI (Cursor, Claude Code)** voor versnelling van repetitieve taken: tests, documentatie, workflow-config — met menselijke review en Maven/pentest als eindcontrole.

**Waar bijsturing nodig was**

- **Scanner-blinde vlek:** Snyk miste gebundelde jQuery/jQuery UI → handmatig HFE-003 in backlog; SPDX miste dezelfde assets.
- **Contextuele her-scoring:** CVSS alleen was misleidend (platform `provided` deps vs. module-eigen JS) → contextuele score in backlog.
- **AI-output:** OpenMRS testcontext, CI-scope en security-claims moesten door het team worden gecorrigeerd (fixtures, scoped `omod`-tests, doc-fixes in [`review-pr15.md`](../review-pr15.md)).
- **Soft gates:** Snyk, zizmor en SonarCloud draaien met `continue-on-error` of token-afhankelijkheid — bewuste keuze voor doorlooptijd, geen harde security-barrière.

**Niet blind vertrouwen**

Scanners en AI vonden niet wat de pentest aantoonde (path traversal, CSRF/IDOR, open redirect). De auditmethodologie ([`00-auditrapport.md`](00-auditrapport.md) §3) combineert daarom vijf sporen: gap-analyse, risicoweging, SAST/SCA, SBOM/patchadvies en penetratietest. Die combinatie — met menselijke triage en teamreview op elke AI-bijdrage — vormt de verantwoording van de realisatie.

---

## Verwijzingen

- Hoofdrapport: [`00-auditrapport.md`](00-auditrapport.md) (§3 Methodologie)
- Onderhoudbaarheid-tooling: [`../onderhoudbaarheid/06-verantwoording-tooling.md`](../onderhoudbaarheid/06-verantwoording-tooling.md)
- AI-verantwoording tests (diepgaand): [`../05-verantwoording-ai-tests.md`](../05-verantwoording-ai-tests.md)
- Interne PR-review (menselijke controle): [`../review-pr15.md`](../review-pr15.md)
- zizmor-resultaten: [`../zizmor.md`](../zizmor.md)
