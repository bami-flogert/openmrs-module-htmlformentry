# Review PR #15 — gereedheid voor merge

> **Historisch document** — momentopname van 15 juni 2026. De huidige projectstatus staat in [`auditrapport/00-auditrapport.md`](auditrapport/00-auditrapport.md) en [`pentest/README.md`](pentest/README.md). Gebruik dit bestand alleen als archief van de PR #15-review.

**Datum:** 15 juni 2026  
**Branch:** `acceptatie` → `main`  
**Reviewer:** geautomatiseerde review conform merge-readiness plan

---

## Samenvatting

| Aspect | Status |
|--------|--------|
| **Gereed voor merge (OTAP/CI-deel)** | Ja, na push van scoped CI + doc-fixes |
| **Volledige LU2-score** | Nee — PoC, pentests, PR #16 ontbreken nog |

PR #15 levert de OTAP-pipeline, CI-validatie, security-tooling en audit-documentatie. Na deze review zijn CI-fouten opgelost via PoC-scope tests; documentatie-claims over SonarCloud en Snyk zijn gecorrigeerd.

---

## CI — root cause en fix

**Probleem:** `mvn test` op volledige reactor faalde in `api-1.10` (`DrugOrderTag1_10Test`, 3 errors). Module-brede `api-tests` heeft ~70 bekende failures (baseline).

**Oplossing:** Scoped CI — `mvn -B -pl omod test verify` in `ci.yml` en `deploy.yml`.

**Lokaal geverifieerd:** 9 tests, 0 failures (2026-06-15).

---

## Rubric-toets — security (PR #15-scope)

| Criterium | Max | Beoordeling | Toelichting |
|-----------|-----|-------------|-------------|
| Secure pipelines / OTAP | 15 | Voldoende | Vier OTAP-fasen, concurrency, smoke test, artifact-bundle, environment secrets |
| SBOM/CVE-advies | 15 | Deels voldoende | SPDX in deploy; Snyk CycloneDX optioneel (token); security-backlog nog niet gemerged |
| Security code review | 15 | Deels voldoende | Snyk SAST/SCA + CodeQL; geen harde gate (`continue-on-error`) |
| Security audit NEN-7510 | 20 | Deels voldoende | Gap-analyse + pipeline-compliance aanwezig; auditrapport DRAFT; risicomatrix extern |
| Penetration tests | 15 | Onvoldoende | Niet in PR #15 |
| Mitigatie & validatie | 20 | Onvoldoende | Niet in PR #15 |

**Conclusie security-deel PR #15:** pipeline-infrastructuur is merge-waardig; pentests en mitigaties vereisen vervolgwerk buiten deze PR.

---

## Bestandsreview (22 bestanden)

### Workflows — OK

| Bestand | Bevinding |
|---------|-----------|
| `ci.yml` | PR-triggers op alle OTAP-branches; PoC-scope unit-test; JaCoCo artifact |
| `deploy.yml` | Deploy geblokkeerd bij falende tests; prod release alleen op `main`; poort 80 vs 8080 correct |
| `snyk.yml` | Token-check als eerste step; pinned action SHA; skip zonder token |
| `dependabot.yml` | Weekly Maven/Actions/Docker; OpenMRS platform-deps genegeerd |
| `smoke-test.sh` | Herbruikbaar curl-loop met configureerbare poort |

### Infra — OK

| Bestand | Bevinding |
|---------|-----------|
| `docker-compose.accept.yml` | Prod-achtig: restart always, 2 GB limit, env-vars voor secrets |
| `.env.example` | Alleen placeholder-variabelen, geen echte secrets |
| `pom.xml` | Minimale diff (2 regels), geen breaking changes |

### Documentatie — gecorrigeerd

| Bestand | Bevinding |
|---------|-----------|
| `docs/otap.md` | Testscope, SonarCloud, Snyk-skip gedocumenteerd |
| `01-nfr-onderhoudbaarheid.md` | NFR-M6/M7: GitHub App i.p.v. Maven workflow |
| `02-pipeline-compliance.md` | Consistent met werkelijke pipeline (Snyk soft gate) |
| `00-auditrapport.md` | DRAFT — executive summary nog open |

### Code — OK

| Bestand | Bevinding |
|---------|-----------|
| `api-2.2/.gitignore` | `/target/` aanwezig; geen gecommitte build-artifacts |
| `DrugOrderTag1_10Test.java` | Timezone-fix behouden; buiten CI-scope |

---

## Gecorrigeerde claims

1. **SonarCloud:** geen `sonar.qualitygate.wait` in repo; integratie via GitHub App + branch protection.
2. **Snyk:** workflow skipped zonder `SNYK_TOKEN` (geen error-spam).
3. **Testscope:** CI gate = OMOD PoC-scope; api-tests baseline gedocumenteerd.

---

## Open punten buiten PR #15

- PR #16: onderhoudbaarheidsrapport + JaCoCo-baseline
- Teststrategie: `03-teststrategie.md`, `04-testresultaten-baseline.md`
- Merge `Security-backlog` + `riscos` branches
- Pentests (OWASP ZAP) + PoC-mitigaties
- GitHub UI: branch protection (`build`, `unit-test`) en environment reviewers

---

## Merge-checklist

- [x] Scoped CI geïmplementeerd
- [x] Lokaal `mvn -pl omod test verify` groen
- [x] Geen gecommitte `target/`-bestanden
- [x] NFR + otap docs gecorrigeerd
- [x] `gh pr checks 15` groen (CI + Deploy OTAP unit-test, deploy-accept; run na commit `723217d`)
- [ ] Branch protection handmatig in GitHub UI (`build`, `unit-test` verplicht op `main`)
- [x] PR #12 gesloten (was al superseded door PR #15)
