# Verantwoording (AI-)tooling — security

**Module:** OpenMRS HTML Form Entry v3.10.0
**Datum:** 2026-06-17
**Status:** In te vullen (kolom *Waarvoor gebruikt* en *Kritische reflectie* door auteur)

> Doel van dit document: per gebruikt hulpmiddel verantwoorden **wat** het is, **waarvoor**
> het in dit security-onderzoek is ingezet, en **kritisch reflecteren** op de waarde en de
> grenzen ervan. Dit dekt het rubriekonderdeel *"realisatie verantwoord, inclusief gebruik
> van (AI)tooling"* en de "Goed"-eis van een kritische reflectie.

---

## 1. AI-tooling

| Tool | Wat het is | Waarvoor gebruikt _(invullen)_ | Kritische reflectie _(invullen)_ |
|------|-----------|-------------------------------|----------------------------------|
| **Claude Code (Opus 4.x)** | AI-codeassistent in de terminal/IDE | _…_ | _… (bijv. waar moest je output corrigeren / verifiëren?)_ |
| **GitHub Copilot** | AI code-completion (zie branch `copilot/…`) | _…_ | _…_ |
| _…_ | _…_ | _…_ | _…_ |

## 2. Security-scanning & SAST

| Tool | Wat het is | Waarvoor gebruikt _(invullen)_ | Kritische reflectie _(invullen)_ |
|------|-----------|-------------------------------|----------------------------------|
| **Snyk (SCA)** | Dependency-/kwetsbaarheidsscan (`snyk test --all-projects`) | _…_ | _… (bijv. miste gebundelde JS-libs → handmatig aangevuld)_ |
| **Snyk Code (SAST)** | Statische code-analyse | _…_ | _…_ |
| **zizmor** | SAST voor GitHub Actions-workflows | _…_ | _…_ |
| **Dependabot** | Geautomatiseerde dependency-updates (Maven/Actions/Docker) | _…_ | _…_ |
| **Dependency review** | PR-check op nieuwe kwetsbare dependencies | _…_ | _…_ |

## 3. SBOM & supply chain

| Tool | Wat het is | Waarvoor gebruikt _(invullen)_ | Kritische reflectie _(invullen)_ |
|------|-----------|-------------------------------|----------------------------------|
| **GitHub Dependency Graph / SPDX** | SBOM-generatie in `deploy.yml` | _…_ | _…_ |
| **CycloneDX (`snyk sbom`)** | Machineleesbare SBOM | _…_ | _…_ |
| **`generate-patch-advice.py`** | Eigen script: SBOM + CVE/CVSS → geprioriteerd patchadvies | _…_ | _… (zelfbouw vs. kant-en-klaar; reproduceerbaarheid)_ |

## 4. Penetratietest-lab

| Tool | Wat het is | Waarvoor gebruikt _(invullen)_ | Kritische reflectie _(invullen)_ |
|------|-----------|-------------------------------|----------------------------------|
| **Docker / Docker Compose** | Geïsoleerd pentest-lab (`Dockerfile.pentest`, `docker-compose.pentest.yml`) | _…_ | _…_ |
| **curl** | Reproduceerbare HTTP-aanvallen en hertests | _…_ | _…_ |
| **Maven (build gepatchte module)** | Bouwt de 3.10.0-broncode → gepatchte `.omod` in het lab | _…_ | _…_ |

## 5. Pipeline & overige

| Tool | Wat het is | Waarvoor gebruikt _(invullen)_ | Kritische reflectie _(invullen)_ |
|------|-----------|-------------------------------|----------------------------------|
| **GitHub Actions (CI/CD)** | OTAP-pipeline (`ci.yml`, `deploy.yml`, `snyk.yml`) | _…_ | _…_ |
| _…_ | _…_ | _…_ | _…_ |

---

## 6. Samenvattende reflectie _(invullen)_

> _Korte synthese: wat leverde (AI-)tooling op, waar zat de meeste waarde, waar moest je
> als auditor bijsturen of niet blind op vertrouwen (bijv. scanner-blinde vlek voor
> gebundelde front-end libs, false positives, contextuele her-scoring)?_

---

## Verwijzingen

- Hoofdrapport: [`00-auditrapport.md`](00-auditrapport.md) (§3 Methodologie)
- Onderhoudbaarheid-tooling: [`../onderhoudbaarheid/06-verantwoording-tooling.md`](../onderhoudbaarheid/06-verantwoording-tooling.md)
- AI-verantwoording tests: [`../05-verantwoording-ai-tests.md`](../05-verantwoording-ai-tests.md)
