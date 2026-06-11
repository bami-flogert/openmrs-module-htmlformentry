# OTAP-pipeline — Ontwikkeling, Test, Acceptatie, Productie

**Module:** OpenMRS HTML Form Entry v3.10.0  
**Datum:** 2026-06-09  
**Scope:** GitHub Actions CI/CD-pipeline en omgevingspromotie  
**Normkader:** NEN-7510:2024-2 (controls A.8.3 · A.8.5 · A.8.15)

---

## Relatie met overige documentatie

| Document | Relatie |
|----------|---------|
| [`auditrapport/00-auditrapport.md`](auditrapport/00-auditrapport.md) | Hoofdrapport security & compliance audit |
| [`auditrapport/02-pipeline-compliance.md`](auditrapport/02-pipeline-compliance.md) | **Bijlage B** — NEN-7510-mapping van pipeline-maatregelen op controls |
| [`auditrapport/01-gap-analyse.md`](auditrapport/01-gap-analyse.md) | **Bijlage A** — gap-analyse module-code (A.8.3 / A.8.5 / A.8.15) |
| [`module-keuze.md`](module-keuze.md) | Motivatie modulekeuze en NEN-7510-relevantie |
| [`onderhoudbaarheid/01-nfr-onderhoudbaarheid.md`](onderhoudbaarheid/01-nfr-onderhoudbaarheid.md) | JaCoCo-coverage en quality gates (NFR-M5 / NFR-T1) |
| [`security.md`](security.md) | Vulnerability disclosure policy |

Dit document beschrijft de **operationele OTAP-keten**: welke branches naar welke omgeving deployen, welke quality gates gelden en welke workflows betrokken zijn. Voor de compliance-mapping per NEN-control zie bijlage B.

---

## Legenda

| Status | Betekenis |
|--------|-----------|
| ✅ Aanwezig | Maatregel is geïmplementeerd in workflow of compose |
| ⚠️ Gedeeltelijk | Aanwezig maar met bekende beperking of handmatige stap |
| ❌ Afwezig | Nog niet geïmplementeerd |

---

## Omgevingen en branches

| OTAP-fase | Branch | GitHub Environment | Docker Compose overlay |
|-----------|--------|--------------------|-------------------------|
| Ontwikkeling | `development` | `dev` | `docker-compose.dev.yml` |
| Test | `pre-release` | `test` | `docker-compose.test.yml` |
| Acceptatie | `acceptatie` | `acceptatie` | `docker-compose.accept.yml` |
| Productie | `main` | `prod` | `docker-compose.prod.yml` |

Alle omgevingen gebruiken [`docker-compose.yml`](../docker-compose.yml) als basis (MySQL 5.6 + OpenMRS reference application).

### Promotie-flow

```
development → pre-release → acceptatie → main
```

Code hoort sequentieel door deze omgevingen te gaan. Elke fase heeft een eigen branch, GitHub Environment en compose-overlay.

---

## Pipeline-overzicht (push naar OTAP-branch)

Workflow: [`.github/workflows/deploy.yml`](../.github/workflows/deploy.yml)

| Stap | Job | Beschrijving | Status |
|------|-----|--------------|--------|
| 1 | `build` | Maven bouwt OMOD (`mvn package -DskipTests`); artefact geüpload | ✅ |
| 2 | `sbom` | SPDX SBOM via GitHub dependency graph (parallel met build) | ✅ |
| 3 | `unit-test` | Maven unit tests + JaCoCo-rapport (parallel met build) | ✅ |
| 4 | `bundle` | OMOD + SBOM + provenance (`build-run.json`) in één artifact | ✅ |
| 5 | `deploy-*` | Deploy naar omgeving die bij de branch hoort | ✅ |
| 6 | Smoke test | OpenMRS bereikbaar via `/openmrs` ([`smoke-test.sh`](../.github/scripts/smoke-test.sh)) | ✅ |
| 7 | Tear down | `docker compose down -v` (ephemeral runner) | ✅ |
| 8 | `publish-prod-release` | Alleen op `main`: OMOD + SBOM + provenance naar GitHub Release | ✅ |

Hetzelfde gebouwde OMOD-artefact en SBOM uit één workflow-run worden gebundeld in `otap-build-bundle` (365 dagen retentie). Deploy downloadt het OMOD; productie publiceert daarnaast naar GitHub Releases (`v3.10.0`).

### Quality gates

| Gate | Wanneer | Actie bij falen | NEN-relevantie |
|------|---------|-----------------|----------------|
| Unit tests | Vóór deploy | Deploy start niet | A.8.15 — aantoonbare kwaliteitscontrole |
| Smoke test | Na deploy | Deploy-job faalt | A.8.15 — deployment-validatie |
| JaCoCo rapport | Na unit tests | Artifact voor audit/evidence | A.8.15 — testbewijs (zie NFR-T1) |
| PR CI | Vóór merge | Merge blokkeerbaar via branch protection | A.8.3 / A.8.5 — gecontroleerde wijzigingen |

Productie smoke test gebruikt poort **80**; overige omgevingen poort **8080**.

---

## Pull request-validatie

Workflow: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)  
Trigger: `pull_request` naar `development`, `pre-release`, `acceptatie` of `main`

| Job | Doel |
|-----|------|
| `build` | Compileert en bouwt OMOD |
| `unit-test` | Zelfde testgate als deploy-pipeline |
| `dependency-review` | Controle op kwetsbare nieuwe dependencies |

Parallel draait [Snyk](../.github/workflows/snyk.yml) (SCA + SAST + CycloneDX SBOM) wanneer `SNYK_TOKEN` is geconfigureerd.

[Dependabot](../.github/dependabot.yml) opent wekelijks PRs voor Maven-, GitHub Actions- en Docker-afhankelijkheden.

---

## Workflows

| Workflow | Bestand | Trigger | Doel |
|----------|---------|---------|------|
| CI | `ci.yml` | `pull_request` | Validatie vóór merge |
| Deploy OTAP | `deploy.yml` | `push` OTAP-branches | Build, SBOM, test, bundle, deploy, prod release |
| Snyk | `snyk.yml` | `push` + `pull_request` | SAST/SCA + CycloneDX SBOM (aanvullend op SPDX in deploy) |

---

## Acceptatie-omgeving

[`docker-compose.accept.yml`](../docker-compose.accept.yml) is productie-achtige staging:

- Geen debug-poort (in tegenstelling tot dev)
- `restart: always` en geheugenlimiet 2 GB (zoals productie)
- Poort 8080 (productie map naar poort 80)

---

## GitHub-configuratie (handmatig)

### Environments

Maak in **Settings → Environments** de omgeving `acceptatie` aan naast `dev`, `test` en `prod`.

| Environment | Aanbevolen instelling |
|-------------|----------------------|
| `dev` | Geen goedkeuring vereist |
| `test` | Optioneel: 1 reviewer |
| `acceptatie` | Vereiste reviewer(s) — UAT-goedkeuring; deployment branch `acceptatie` |
| `prod` | Vereiste reviewer(s); deployment branch `main` |

Gebruik per omgeving eigen secrets (`MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`) waar mogelijk.

### Branch protection

Onder **Settings → Branches** — vereiste checks per OTAP-branch:

| Branch | Vereiste CI-checks |
|--------|-------------------|
| `development` | `build`, `unit-test` |
| `pre-release` | `build`, `unit-test` |
| `acceptatie` | `build`, `unit-test` |
| `main` | `build`, `unit-test` |

Optioneel ook `dependency-review` en Snyk als required checks.

---

## Bekende beperkingen

| Beperking | Impact | Status |
|-----------|--------|--------|
| Ephemeral GitHub-hosted runners | Geen persistente OTAP-servers; deploy valideert proces, niet productie-hosting | ⚠️ |
| Dev-compose hardcoded wachtwoorden | `docker-compose.dev.yml` gebruikt `root` / `openmrs` i.p.v. secrets | ⚠️ |
| Docker image `:latest` | OpenMRS-image niet gepind op digest — reproduceerbaarheid | ⚠️ |
| CycloneDX SBOM alleen via Snyk | SPDX zit in deploy-bundle; CycloneDX apart in `snyk.yml` | ⚠️ |
| Legacy Travis CI | [`.travis.yml`](../.travis.yml) nog aanwezig naast GitHub Actions | ⚠️ |

Voor echte gescheiden OTAP-hosting zijn self-hosted runners of deploy naar externe VMs nodig.

---

## Openstaande verbeteringen

- Docker image pinning in `docker-compose.yml`
- Dev-compose secrets via environment variables
- Concurrency-bescherming op deploy-jobs
- Verwijderen van legacy `.travis.yml`

Zie ook de security backlog in [`auditrapport/00-auditrapport.md`](auditrapport/00-auditrapport.md) (bijlage I, nog te maken).
