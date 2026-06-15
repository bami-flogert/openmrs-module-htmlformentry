# zizmor — GitHub Actions security scan

**Module:** OpenMRS HTML Form Entry v3.10.0  
**Datum:** 2026-06-15  
**Scope:** `.github/workflows/`, `.github/dependabot.yml`  
**Gerelateerd:** [`otap.md`](otap.md) · [`auditrapport/02-pipeline-compliance.md`](auditrapport/02-pipeline-compliance.md)

---

## Wat is zizmor?

[zizmor](https://zizmor.sh/) is een **statische analysetool (SAST) voor GitHub Actions**. Waar Snyk en SonarCloud vooral Java-code en Maven-dependencies scannen, kijkt zizmor naar de **CI/CD-configuratie zelf**: workflow-YAML, Dependabot en composite actions.

Typische bevindingen:

| Categorie | Voorbeeld |
|-----------|-----------|
| Supply chain | Actions gepind op `@v4` i.p.v. commit-SHA (`unpinned-uses`) |
| Injectie | `${{ github.head_ref }}` in een `run:`-blok (`template-injection`) |
| Credentials | `actions/checkout` zonder `persist-credentials: false` (`artipacked`) |
| Permissions | Te brede `permissions:` op workflow-niveau |
| Dependabot | Geen cooldown tussen updates (`dependabot-cooldown`) |

Zizmor draait **offline** op lokale bestanden. Met een `GH_TOKEN` kan het extra online-checks doen (bijv. bekende advisories op actions). Zie [documentatie](https://docs.zizmor.sh/).

---

## Waarom in dit project?

De OTAP-pipeline ([`deploy.yml`](../.github/workflows/deploy.yml), [`ci.yml`](../.github/workflows/ci.yml), [`snyk.yml`](../.github/workflows/snyk.yml)) verwerkt secrets, bouwt artefacten en deployt naar vier omgevingen. Een fout in workflow-YAML kan leiden tot credential-lekken of supply-chain-aanvallen — los van kwetsbaarheden in de module-code.

Zizmor vult de bestaande tooling aan:

| Tool | Wat het scant |
|------|----------------|
| Snyk | Java SAST/SCA, CycloneDX SBOM |
| SonarCloud | Java-kwaliteit en security smells |
| Dependency review | Nieuwe kwetsbare deps op PRs |
| **zizmor** | **GitHub Actions-workflows en Dependabot** |

Het is bedoeld als **lokale pre-check** en auditbewijs (NEN-7510 A.8.15 — statische analyse van de pipeline). Het staat nog niet als verplichte CI-gate; zie [Vervolg](#vervolg).

---

## Installatie

Zie [docs.zizmor.sh — installation](https://docs.zizmor.sh/installation/).

**macOS (Homebrew):**

```bash
brew install zizmor
```

**Cargo (Rust):**

```bash
cargo install zizmor
```

Controleer de installatie:

```bash
zizmor --version
```

---

## Gebruik in deze repository

Vanuit de **repository-root**:

```bash
zizmor .
```

Dat is de standaard manier: geef zizmor een map en het verzamelt automatisch:

- `.github/workflows/*.yml`
- `.github/dependabot.yml`
- eventuele `action.yml` (composite actions) in submappen

In dit project worden drie workflows en Dependabot gescand:

| Bestand | Rol |
|---------|-----|
| [`ci.yml`](../.github/workflows/ci.yml) | PR-build, tests, dependency review, SonarCloud |
| [`deploy.yml`](../.github/workflows/deploy.yml) | OTAP-build, SBOM, deploy, prod release |
| [`snyk.yml`](../.github/workflows/snyk.yml) | Snyk SAST/SCA + patchadvies |
| [`dependabot.yml`](../.github/dependabot.yml) | Wekelijkse updates Maven / Actions / Docker |

### Nuttige opties

```bash
# Alleen workflows (geen Dependabot)
zizmor --collect=workflows .

# Alleen high severity
zizmor --min-severity=high .

# Volledig offline (geen GitHub API)
zizmor --offline .

# Experimentele auto-fix (veilige fixes)
zizmor --fix=safe .

# SARIF voor GitHub code scanning
zizmor --format=sarif . > zizmor.sarif
```

Exitcode: `0` = geen bevindingen boven drempel, `14` = bevindingen gevonden (normaal bij een eerste scan).

---

## Resultaat (baseline 2026-06-15)

Uitvoering: `zizmor .` (v1.25.2), vanaf repository-root.

**Samenvatting:** 58 zichtbare bevindingen — 43 high, 15 medium (plus 33 onderdrukte regels in de standaard persona).

| Audit-regel | Ernst | Aantal | Korte uitleg |
|-------------|-------|--------|--------------|
| `unpinned-uses` | error (high) | 35 | Actions gebruiken `@v4`-tags i.p.v. immutable commit-SHA |
| `artipacked` | warning (medium) | 12 | `actions/checkout` zonder `persist-credentials: false` |
| `template-injection` | error (high) | 4 | `${{ github.head_ref }}` / `base_ref` in Sonar `run:`-blok (`ci.yml`) |
| `cache-poisoning` | error (high) | 4 | Maven-cache op workflows die runtime-artefacten publiceren |
| `dependabot-cooldown` | warning (medium) | 3 | Geen cooldown in [`dependabot.yml`](../.github/dependabot.yml) |

Per bestand:

- **`ci.yml`** — template-injection (Sonar PR-parameters), unpinned actions, artipacked, cache-poisoning
- **`deploy.yml`** — unpinned actions, artipacked, cache-poisoning
- **`snyk.yml`** — unpinned actions, artipacked, cache-poisoning
- **`dependabot.yml`** — cooldown ontbreekt voor maven, github-actions en docker

### Voorbeeld (template-injection)

Zizmor markeert dit patroon in `ci.yml` omdat branch-namen van een PR theoretisch shell-metacharakters kunnen bevatten:

```yaml
-Dsonar.pullrequest.branch=${{ github.head_ref }}
```

Mitigatie: waarden via `env:` doorgeven of `format()` gebruiken i.p.v. directe interpolatie in `run:`. Zie [template-injection audit](https://docs.zizmor.sh/audits/#template-injection).

### Voorbeeld (unpinned-uses)

```yaml
- uses: actions/checkout@v4   # zizmor wil: actions/checkout@<commit-sha>
```

Tags zijn verplaatsbaar; SHA-pinnen beschermt tegen supply-chain-wijzigingen. Zie [unpinned-uses audit](https://docs.zizmor.sh/audits/#unpinned-uses).

---

## Bevindingen negeren of configureren

**Eén regel negeren** (in de workflow):

```yaml
- uses: actions/checkout@v4  # zizmor: ignore[unpinned-uses]
```

**Projectbreed:** `.github/zizmor.yml` — zie [Ignoring results](https://docs.zizmor.sh/usage/#ignoring-results).

Gebruik ignores spaarzaam en noteer de reden (audittrail, control 8.8).

---

## Vervolg

| Stap | Status |
|------|--------|
| Baseline vastleggen (`zizmor .` lokaal) | ✅ Dit document |
| CI-job in [`ci.yml`](../.github/workflows/ci.yml) (`zizmor`-job) | ✅ Aanwezig — `continue-on-error: true` tot bevindingen zijn opgelost |
| Hoog-risico bevindingen beoordelen (`template-injection`, `unpinned-uses`) | Open — daarna `continue-on-error` verwijderen |
| Optioneel: SARIF upload (`advanced-security: true`) | Niet geïmplementeerd |

Voor CI-integratie en drempels: zie [zizmor quickstart](https://docs.zizmor.sh/quickstart/) en [usage](https://docs.zizmor.sh/usage/).

---

## Checklist (lokaal)

- [ ] `zizmor` geïnstalleerd (`zizmor --version`)
- [ ] Vanaf repo-root: `zizmor .`
- [ ] High-severity bevindingen doorgenomen
- [ ] Na workflow-wijziging opnieuw gescand
