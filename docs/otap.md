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

1. **Build** — Maven bouwt het OMOD-artefact (`mvn package`).
2. **Deploy** — Alleen de job die bij de branch hoort, start Docker Compose met de juiste overlay.

Hetzelfde gebouwde artefact wordt per run geüpload en gedownload in de deploy-job (immutable promotion binnen één workflow-run).

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

- **Deploy OTAP** — [`.github/workflows/deploy.yml`](../.github/workflows/deploy.yml)
- **SBOM** — [`.github/workflows/sbom.yml`](../.github/workflows/sbom.yml) (ook op `acceptatie`)

## Geplande verbeteringen

- Unit tests als quality gate vóór deploy
- PR-validatie workflow (`pull_request`)
- SBOM koppelen aan build-artefact
- Post-deploy smoke checks
- Dependabot en dependency scanning
