# Presentatie-outline — LU2 onderhoudbaarheid

**Duur:** 30 min presentatie + 15 min vragen  
**Datum:** juni 2026 · **Branch:** `acceptatie`  
**Doel:** rubric-criteria verantwoorden (CGI); demo PoC + voor/na-metrieken

---

## Rolverdeling (CGI)

| Teamlid | Rubric-onderdeel | Pt | Lead in presentatie |
|---------|------------------|-----|---------------------|
| **A** | Testopzet & testresultaten | 20 | Characterization tests T1–T9, CI, SonarCloud, regressie-subset |
| **A** | Deel validatie (metrieken) | — | Sonar/JaCoCo-cijfers, Quality Gate, PR #31/#37 demo |
| **B** | Aangepast ontwerp | 20 | As-is/to-be, alternatieven, Extract Method-keuze |
| **B** | Validatie (verhaal) | 20 | NFR M1–M6 beoordeling, regressiebewijs |
| **B** | PoC-verantwoording + AI-reflectie | 10 | AI vs mens, trade-offs, [`06-verantwoording-tooling.md`](onderhoudbaarheid/06-verantwoording-tooling.md) |
| **Beide** | Analyse onderhoudbaarheid | 20 | Baseline rapport + hotspots (kort) |
| **Beide** | Verbeteringen / prioritering | 10 | Rapport §5, PoC #1 keuze |

---

## Tijdschema (30 min)

| Min | Onderdeel | Wie | Bron / demo |
|-----|-----------|-----|-------------|
| 0–3 | Intro: module, scope, OTAP | A | [`module-keuze.md`](module-keuze.md), [`otap.md`](otap.md) |
| 3–8 | Baseline-analyse & hotspots | B | [`onderhoudbaarheidsrapport.md`](onderhoudbaarheidsrapport.md) §3–5 |
| 8–14 | **Aangepast ontwerp** (alternatieven, keuze) | **B** | [`05-ontwerp-refactoring.md`](onderhoudbaarheid/05-ontwerp-refactoring.md) — mermaid flowcharts |
| 14–20 | **Teststrategie & PoC-tests** | **A** | [`03-teststrategie.md`](03-teststrategie.md); T1–T9 + ExtractedMethodsTest |
| 20–25 | **Validatie voor/na + NFR** | **B** | [`07-validatie-voor-na.md`](07-validatie-voor-na.md) §4 samenvattingstabel |
| 25–28 | **CI/Sonar demo** | **A** | GitHub Actions PR #37; Sonar dashboard; JaCoCo artifact |
| 28–30 | AI-reflectie & conclusie | **B** | [`06-verantwoording-tooling.md`](onderhoudbaarheid/06-verantwoording-tooling.md) §5 |
| 30–45 | Vragen (CGI) | Beide | Zie § CGI-voorbereiding |

---

## Demo-checklist

- [ ] SonarCloud dashboard: CC voor/na, 0 new S3776, 5 Major smells
- [ ] GitHub Actions: groene `unit-test` + `SonarCloud Analysis` (link PR #31 of #37)
- [ ] JaCoCo: `~63%` `HtmlFormEntryController` (artifact of lokaal rapport)
- [ ] Code: `getFormEntrySession` → `resolveMode`, `resolveFormEntryContext`, … (IDE)
- [ ] Regressie: 67 omod + 58 api-tests subset (cijfers uit [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md))

---

## CGI-voorbereiding (vragen per persoon)

### Teamlid A — moet kunnen uitleggen

1. Waarom characterization tests vóór refactor (Fowler)?
2. Wat doet de regressie-subset (5 klassen, 58 tests) en waarom niet de volledige api-tests?
3. Hoe werkt `sonar.qualitygate.wait=true` en wat gebeurt bij FAILED?
4. JaCoCo-import fix (PR #37): wat was het probleem en de oplossing?
5. Welke metrieken komen uit Sonar vs JaCoCo lokaal?

### Teamlid B — moet kunnen uitleggen

1. Waarom Extract Method i.p.v. Extract Class (`FormEntryRequestResolver`)?
2. Hoe beoordeel je NFR-M1, M3, M4 voor de PoC (voldaan/niet)?
3. Waarom is controller ~567 LOC acceptabel ondanks oorspronkelijk < 200 LOC-doel?
4. Welke rol had AI bij refactor vs tests — en waar liep het mis?
5. Wat is de traceerbaarheid van ontwerp → implementatie → validatie?

---

## Documentindex (snel navigeren tijdens vragen)

| Document | Pad |
|----------|-----|
| Ontwerp | [`onderhoudbaarheid/05-ontwerp-refactoring.md`](onderhoudbaarheid/05-ontwerp-refactoring.md) |
| Validatie | [`07-validatie-voor-na.md`](07-validatie-voor-na.md) |
| Testresultaten / handoff | [`04-testresultaten-baseline.md`](04-testresultaten-baseline.md) |
| Verantwoording tooling | [`onderhoudbaarheid/06-verantwoording-tooling.md`](onderhoudbaarheid/06-verantwoording-tooling.md) |
| AI tests | [`05-verantwoording-ai-tests.md`](05-verantwoording-ai-tests.md) |
| NFR | [`01-nfr-onderhoudbaarheid.md`](01-nfr-onderhoudbaarheid.md) |
