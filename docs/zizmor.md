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

## Resultaat

### Baseline (2026-06-15)

Eerste scan: 58 zichtbare bevindingen — 43 high, 15 medium.

### Na remediatie (2026-06-15)

Uitvoering: `zizmor .` (v1.25.2), vanaf repository-root.

**Samenvatting:** 40 open bevindingen — 40 high (plus onderdrukte regels in de standaard persona).

| Audit-regel | Ernst | Was | Nu | Status |
|-------------|-------|-----|-----|--------|
| `template-injection` | error (high) | 4 | 0 | ✅ Opgelost — PR-variabelen via `env:` (`ci.yml`); provenance JSON via `env:` (`deploy.yml`) |
| `artipacked` | warning (medium) | 12 | 0 | ✅ Opgelost — `persist-credentials: false` op alle `actions/checkout`-stappen |
| `dependabot-cooldown` | warning (medium) | 3 | 0 | ✅ Opgelost — `cooldown.default-days: 7` in [`dependabot.yml`](../.github/dependabot.yml) |
| `unpinned-uses` | error (high) | 35 | 36 | Open — SHA-pinnen van alle actions (grote wijziging; Dependabot houdt tags actueel) |
| `cache-poisoning` | error (high) | 4 | 4 | Open — Maven-cache op publish-workflows; lage confidence, structurele wijziging nodig |

Per bestand (open):

- **`ci.yml`** — unpinned actions, cache-poisoning
- **`deploy.yml`** — unpinned actions, cache-poisoning
- **`snyk.yml`** — unpinned actions, cache-poisoning

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
| CI-job in [`ci.yml`](../.github/workflows/ci.yml) (`zizmor`-job) | ✅ Aanwezig — `continue-on-error: true` zolang `unpinned-uses` / `cache-poisoning` bewust niet zijn gemitigeerd |
| Eenvoudige remediatie (`template-injection`, `artipacked`, `dependabot-cooldown`) | ✅ Opgelost (2026-06-15) |
| SHA-pinnen actions (`unpinned-uses`, 36×) | Open — aparte PR |
| Cache-poisoning (Maven-cache op publish-workflows) | Open — beoordelen of accepteren |
| Optioneel: SARIF upload (`advanced-security: true`) | Niet geïmplementeerd |

Voor CI-integratie en drempels: zie [zizmor quickstart](https://docs.zizmor.sh/quickstart/) en [usage](https://docs.zizmor.sh/usage/).

---

## Besluit (audittrail)

De twee open high-categorieën worden in deze PoC **gedocumenteerd geaccepteerd** en in een aparte wijziging opgepakt:

- **`unpinned-uses`**: actions staan op tag (`@v4`) i.p.v. commit-SHA. Dit is een supply-chain hardening maatregel met een relatief grote diff (veel regels, regelmatig onderhoud). In deze repo mitigeert Dependabot het risico deels door tags actueel te houden, maar SHA-pinnen blijft de voorkeur.
- **`cache-poisoning`**: zizmor rapporteert mogelijke poisoning van Maven caches op publish-workflows. Dit vereist structurele workflow-wijzigingen en moet per job/cache-key beoordeeld worden.

**CI-gate:** de zizmor-job blijft `continue-on-error: true` tot één van deze twee keuzes is gemaakt:

1. **Mitigeren**: SHA-pinnen + cache-structuur aanpassen, daarna `continue-on-error: false` (harde gate), of
2. **Formeel accepteren**: een projectbrede zizmor-config (`.github/zizmor.yml`) met onderbouwing per suppressie + een vast reviewmoment (bijv. per release).

---

## Checklist (lokaal)

- [ ] `zizmor` geïnstalleerd (`zizmor --version`)
- [ ] Vanaf repo-root: `zizmor .`
- [ ] High-severity bevindingen doorgenomen
- [ ] Na workflow-wijziging opnieuw gescand
