# OTAP-pipeline

Dit document beschrijft de OTAP-omgevingen (Ontwikkeling, Test, Acceptatie, Productie) en hoe deployments via GitHub Actions verlopen.

## Omgevingen en branches

| OTAP-fase      | Branch        | GitHub Environment | Docker Compose overlay        |
|----------------|---------------|--------------------|-------------------------------|
| Ontwikkeling   | `development` | `dev`              | `docker-compose.dev.yml`      |
| Test           | `pre-release` | `test`             | `docker-compose.test.yml`     |
| Acceptatie     | `acceptatie`  | `acceptatie`       | `docker-compose.accept.yml`   |
| Productie      | `main`        | `prod`             | `docker-compose.prod.yml`     |

Alle omgevingen gebruiken [`docker-compose.yml`](../docker-compose.yml) als basis (MySQL + OpenMRS reference application).

## Promotie-flow

Code hoort sequentieel door de omgevingen te gaan:

```
development → pre-release → acceptatie → main
```

Elke push naar een OTAP-branch triggert [`.github/workflows/deploy.yml`](../.github/workflows/deploy.yml):

1. **Build** — Maven bouwt het OMOD-artefact (`mvn package -DskipTests`).
2. **Unit-test** — Maven draait alle unit tests (`mvn test`). Deploy wordt geblokkeerd als tests falen.
3. **Deploy** — Alleen de job die bij de branch hoort, start Docker Compose met de juiste overlay.
4. **Smoke test** — Na deploy wordt gecontroleerd of OpenMRS bereikbaar is (`/openmrs` endpoint).
5. **Tear down** — Containers worden gestopt (ephemeral runner).

`build` en `unit-test` draaien parallel. Alle deploy-jobs hebben `needs: [build, unit-test]`.

Hetzelfde gebouwde artefact wordt per run geüpload en gedownload in de deploy-job (immutable promotion binnen één workflow-run).

### Quality gates

| Gate            | Wanneer        | Actie bij falen                          |
|-----------------|----------------|------------------------------------------|
| Unit tests      | Vóór deploy    | Deploy-jobs starten niet                   |
| Smoke test      | Na deploy      | Deploy-job faalt; geen succesvolle release |
| JaCoCo rapport  | Na unit tests  | Geüpload als artifact (audit/evidence)     |

Het smoke-test script staat in [`.github/scripts/smoke-test.sh`](../.github/scripts/smoke-test.sh). Productie gebruikt poort 80; overige omgevingen poort 8080.

## Acceptatie-omgeving

De acceptatie-omgeving (`docker-compose.accept.yml`) is bedoeld als productie-achtige staging:

- Geen debug-poort (in tegenstelling tot dev)
- `restart: always` en geheugenlimiet van 2 GB (zoals productie)
- Poort 8080 (productie gebruikt poort 80)

## GitHub-configuratie (handmatig)

Maak in de repository **Settings → Environments** de omgeving `acceptatie` aan naast de bestaande `dev`, `test` en `prod`.

Aanbevolen bescherming:

| Environment  | Aanbevolen instelling                          |
|--------------|------------------------------------------------|
| `dev`        | Geen goedkeuring vereist                       |
| `test`       | Optioneel: 1 reviewer                          |
| `acceptatie` | Vereiste reviewer(s) — UAT-goedkeuring         |
| `prod`       | Vereiste reviewer(s) — productie-deploy        |

Gebruik per omgeving eigen secrets (`MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`) waar mogelijk.

## Beperking: ephemeral runners

Deploy-jobs draaien op GitHub-hosted runners (`ubuntu-latest`). Containers worden gestart tijdens de job en verdwijnen wanneer de job eindigt. Dit valideert het deployment-proces, maar er zijn geen persistente OTAP-servers.

Voor echte gescheiden omgevingen zijn self-hosted runners of deploy naar externe hosts nodig (buiten scope van de huidige setup).

## Gerelateerde workflows

| Workflow | Trigger | Doel |
|----------|---------|------|
| [CI](../.github/workflows/ci.yml) | `pull_request` naar OTAP-branches | Build + unit tests vóór merge |
| [Deploy OTAP](../.github/workflows/deploy.yml) | `push` naar OTAP-branches | Build, test, deploy per omgeving |
| [SBOM](../.github/workflows/sbom.yml) | `push` naar OTAP-branches | SPDX SBOM via GitHub dependency graph |
| [Snyk](../.github/workflows/snyk.yml) | `push` + `pull_request` | SAST/SCA en CycloneDX SBOM (vereist `SNYK_TOKEN` secret) |

### Pull request-validatie

Pull requests naar `development`, `pre-release`, `acceptatie` of `main` starten de **CI**-workflow:

1. **Build** — controleert of het project compileert en het OMOD gebouwd kan worden.
2. **Unit-test** — dezelfde testgate als bij deploy; merge kan geblokkeerd worden via branch protection.
3. **Dependency review** — GitHub controleert of nieuwe dependencies bekende kwetsbaarheden introduceren.

Snyk draait parallel op PRs (SCA + SAST) wanneer `SNYK_TOKEN` is geconfigureerd.

### Dependabot

[`.github/dependabot.yml`](../.github/dependabot.yml) opent wekelijks PRs voor Maven-, GitHub Actions- en Docker-afhankelijkheden.

## GitHub branch protection (aanbevolen)

Stel per OTAP-branch in onder **Settings → Branches**:

| Branch | Vereiste checks |
|--------|-----------------|
| `development` | `build`, `unit-test` (CI workflow) |
| `pre-release` | `build`, `unit-test` |
| `acceptatie` | `build`, `unit-test` |
| `main` | `build`, `unit-test` |

Optioneel ook `dependency-review` en Snyk als required checks.

## Geplande verbeteringen

- SBOM koppelen aan build-artefact (één traceerbare build-run)
- Docker image pinning in `docker-compose.yml`
- Dev-compose secrets via environment variables
- Concurrency-bescherming op deploy-jobs
- Verwijderen van legacy `.travis.yml`
