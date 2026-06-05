# Mini-complianceverslag — Pipeline & NEN-7510:2024-2

**Module:** OpenMRS HTML Form Entry v3.10.0  
**Datum:** 2026-06-03  
**Scope:** GitHub Actions CI/CD-pipeline

---

| NEN-7510 Control | Omschrijving | Pipeline-maatregel | Bewijs | Status |
|---|---|---|---|---|
| **A.8.3** Toegangsbeveiliging | Toegang tot omgevingen beperken op basis van autorisatie | GitHub Environments (`dev` / `test` / `prod`) — alleen gemachtigde branches mogen deployen | `deploy.yml:35` `if: github.ref == 'refs/heads/development'`<br>`deploy.yml:54` `if: github.ref == 'refs/heads/pre-release'`<br>`deploy.yml:72` `if: github.ref == 'refs/heads/main'` | ⚠️ Gedeeltelijk — branch-guards aanwezig; environment protection rules (required reviewers) moeten nog handmatig via GitHub UI worden ingesteld |
| **A.8.3** Toegangsbeveiliging | Geheimen niet in plaintext opslaan | Databasewachtwoorden via GitHub Secrets, nooit hardcoded in workflow | `deploy.yml:49-50` `secrets.MYSQL_ROOT_PASSWORD` / `secrets.MYSQL_PASSWORD`<br>`docker-compose.prod.yml` — credentials via `${MYSQL_PASSWORD}` env-var | ✅ Aanwezig |
| **A.8.5** Authenticatie | Alleen geauthenticeerde gebruikers kunnen deployen | GitHub Actions vereist authenticatie via GITHUB_TOKEN; deploy-jobs draaien alleen na geslaagde `build`-job | `deploy.yml:12-31` build-job als verplichte dependency (`needs: build`)<br>`sbom.yml:12` `permissions: contents: read` | ✅ Aanwezig |
| **A.8.5** Authenticatie | Minimale rechten voor pipeline-token | SBOM-workflow gebruikt `contents: read` — minste privilege dat volstaat | `sbom.yml:11-12` `permissions: contents: read` | ✅ Aanwezig — deploy-workflow mist expliciete `permissions`-blok |
| **A.8.15** Logging | Artefacten en bouwresultaten bewaren als auditspoor | OMOD-artefact geüpload als GitHub Actions artifact na elke build | `deploy.yml:26-31` `actions/upload-artifact@v4` met `retention-days: 1` | ⚠️ Gedeeltelijk — artefact aanwezig, maar retentie slechts 1 dag (NEN-7510 vereist langere bewaarplicht voor medische software) |
| **A.8.15** Logging | SBOM genereren als onderdeel van audittrail | Automatische SBOM-export bij elke push naar main/development/pre-release | `sbom.yml:22-24` `gh api /repos/.../dependency-graph/sbom`<br>`sbom.yml:28-32` upload als `sbom-artifact` | ✅ Aanwezig |
| **A.8.15** Logging | Reproduceerbare bouwomgeving vastleggen | Vaste Java-versie en Maven-cache garanderen reproduceerbare builds | `deploy.yml:16-21` `actions/setup-java@v4` met `java-version: '8'` en `cache: maven` | ✅ Aanwezig |
| **A.8.15** Logging | Pijplijn-logs bewaren en raadpleegbaar houden | GitHub Actions bewaart workflow-logs per run (standaard 90 dagen) | GitHub UI — alle runs raadpleegbaar via *Actions* tab van de repository | ✅ Aanwezig — afhankelijk van GitHub-standaardinstellingen, niet actief geconfigureerd |

---