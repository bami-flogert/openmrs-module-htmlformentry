# Mini-complianceverslag — Pipeline & NEN-7510:2024-2

**Module:** OpenMRS HTML Form Entry v3.10.0  
**Datum:** 2026-06-09  
**Scope:** GitHub Actions CI/CD-pipeline (OTAP)  
**Detaildocumentatie:** [`../otap.md`](../otap.md)

---

| NEN-7510 Control | Omschrijving | Pipeline-maatregel | Bewijs | Status |
|---|---|---|---|---|
| **A.8.3** Toegangsbeveiliging | Toegang tot omgevingen beperken op basis van autorisatie | Vier OTAP-omgevingen via GitHub Environments (`dev` / `test` / `acceptatie` / `prod`); deploy alleen op matching branch | `deploy.yml` — `if: github.ref == 'refs/heads/development'` etc.<br>`deploy-accept` op `acceptatie`<br>[`otap.md`](../otap.md) — omgevingstabel | ⚠️ Gedeeltelijk — branch-guards aanwezig; required reviewers en deployment-branch rules moeten in GitHub UI worden ingesteld |
| **A.8.3** Toegangsbeveiliging | Geheimen niet in plaintext opslaan | Databasewachtwoorden via GitHub Secrets in test/acceptatie/prod | `deploy.yml` — `secrets.MYSQL_ROOT_PASSWORD` / `secrets.MYSQL_PASSWORD`<br>`docker-compose.prod.yml` — `${MYSQL_PASSWORD}` | ⚠️ Gedeeltelijk — `docker-compose.dev.yml` bevat nog hardcoded `root` / `openmrs` |
| **A.8.3** Toegangsbeveiliging | Wijzigingen controleren vóór promotie | PR-workflow met build + tests + dependency review | `ci.yml` — `pull_request` naar OTAP-branches<br>`dependency-review-action@v4` | ✅ Aanwezig — branch protection moet checks verplicht stellen |
| **A.8.5** Authenticatie | Alleen geauthenticeerde gebruikers kunnen deployen | GitHub Actions vereist authenticatie; deploy na geslaagde build én unit-test | `deploy.yml` — `needs: [build, unit-test]` | ✅ Aanwezig |
| **A.8.5** Authenticatie | Minimale rechten voor pipeline-token | Workflows gebruiken `permissions: contents: read` | `deploy.yml:11-12`, `ci.yml`, `sbom.yml`, `snyk.yml` | ✅ Aanwezig |
| **A.8.15** Logging | Artefacten bewaren als auditspoor | OMOD-artefact + JaCoCo-rapport + Snyk-resultaten als GitHub Actions artifacts | `deploy.yml` — `retention-days: 365` (OMOD)<br>`deploy.yml` — `jacoco-report` (30 dagen)<br>`snyk.yml` — `snyk-results` artifact | ✅ Aanwezig — JaCoCo-retentie korter dan OMOD; overweeg verlenging voor audit |
| **A.8.15** Logging | SBOM als onderdeel van audittrail | SPDX SBOM (GitHub) + CycloneDX SBOM (Snyk) bij push/PR | `sbom.yml` — `gh api .../dependency-graph/sbom`<br>`snyk.yml` — `snyk sbom` | ✅ Aanwezig — nog niet gekoppeld aan één build-run |
| **A.8.15** Logging | Statische analyse / kwetsbaarheidsscan | Snyk SAST (code) + SCA (dependencies) | `snyk.yml` — `snyk test`, `snyk code test` | ⚠️ Gedeeltelijk — `continue-on-error: true`; vereist `SNYK_TOKEN` |
| **A.8.15** Logging | Reproduceerbare bouwomgeving | Vaste JDK 8 (Temurin) + Maven-cache | `deploy.yml` / `ci.yml` — `setup-java@v4`, `java-version: '8'` | ✅ Aanwezig |
| **A.8.15** Logging | Deployment-validatie vastleggen | Post-deploy smoke test + workflow-logs | `smoke-test.sh` — curl `/openmrs`<br>GitHub Actions — run logs (standaard 90 dagen) | ✅ Aanwezig |
| **A.8.15** Logging | Supply-chain updates monitoren | Dependabot voor Maven, GitHub Actions en Docker | `.github/dependabot.yml` | ✅ Aanwezig |
| **—** OTAP-scheiding | Vier fasen Ontwikkeling → Test → Acceptatie → Productie | Branch-per-omgeving + compose-overlays | `acceptatie` branch + `docker-compose.accept.yml`<br>[`otap.md`](../otap.md) | ✅ Aanwezig |
| **—** OTAP-scheiding | Persistente gescheiden omgevingen | — | Deploy op ephemeral `ubuntu-latest` runners | ❌ Afwezig — procesvalidatie alleen; zie [`otap.md`](../otap.md) |

---

## Samenvatting pipeline-compliance

| Categorie | Eindbeoordeling |
|-----------|-----------------|
| Toegangsbeveiliging (A.8.3) | ⚠️ Gedeeltelijk |
| Authenticatie pipeline (A.8.5) | ✅ Grotendeels aanwezig |
| Logging & audittrail (A.8.15) | ⚠️ Gedeeltelijk — sterker dan initieel; Snyk niet hard gate |
| OTAP-structuur | ✅ Vier fasen aanwezig; ⚠️ geen persistente hosts |

**Prioritaire vervolgacties:** GitHub Environment protection rules (acceptatie/prod), dev-secrets uit compose halen, Snyk als harde gate overwegen, SBOM aan build-run koppelen.
