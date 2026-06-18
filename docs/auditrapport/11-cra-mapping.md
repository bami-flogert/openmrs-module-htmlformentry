# CRA-mapping (bijlage H)

**Module:** OpenMRS HTML Form Entry v3.10.0
**Normkader:** EU Cyber Resilience Act
**Doel:** elke CRA-eis koppelen aan de module-invulling, het bewijs in deze
audit en de bijbehorende NEN-7510-control, met een expliciete gap waar nog werk ligt.

---

## 1. Annex I — Deel I: Essentiële cybersecurity-eisen

| # | CRA-eis | bewijs | NEN-7510 | Status | Gap |
|---|--------------------------|---------------------------|----------|--------|-----|
| 1 | Geen bekende exploiteerbare kwetsbaarheden bij release | HFE-01/02/03 gemitigeerd + hertest ([pentest](../pentest/README.md)); SCA-backlog | A.8.8 | ⚠️ | Maven/Docker-CVE's nog open ([patchadvies](07-patchadvies.md)) |
| 2 | Secure-by-default configuratie | Whitelist-basismap preview ([HFE-01 ná](../pentest/bevinding-hfe-01-na.md)); fail-closed autorisatie | A.8.3 · A.8.5 | ⚠️ | CSRF nog niet modulebreed default |
| 3 | Bescherming tegen onbevoegde toegang (authn/authz) | `requirePrivilege(...)` ([HFE-02 ná](../pentest/bevinding-hfe-02-na.md)); privilege-matrix ([security.md](../security.md)) | A.8.3 · A.8.5 | ⚠️ | `@Authorized` op service-laag ontbreekt ([gap A.8.3](01-gap-analyse.md)) |
| 4 | Vertrouwelijkheid (encryptie at rest/in transit) | TLS via platform; PII uit logs ([HFE-04 ná](../pentest/bevinding-hfe-04-na.md)) | A.8.24 | ⚠️ | Geen modulespecifieke encryptie |
| 5 | Integriteit van data/commando's/config | Fail-closed void + IDOR/CSRF-fix ([HFE-02 ná](../pentest/bevinding-hfe-02-na.md)) | A.8.3 | ✅ | — |
| 6 | Dataminimalisatie (alleen noodzakelijke gegevens) | Audit-log alleen `patientId`/`userId`/IDs ([08-logging](../08-logging.md)) | A.8.15 | ✅ | — |
| 7 | Beschikbaarheid / weerbaarheid tegen (D)DoS | Steunt op platform/infra | — | ⚠️ | Niet modulespecifiek getoetst |
| 8 | Minimaliseren impact op andere systemen/netwerken | Module draait binnen OpenMRS-trust-zone ([attack surface](../pentest/00-attack-surface-mapping.md)) | — | ✅ | — |
| 9 | Beperken van het aanvalsoppervlak | Attack-surface-map + high-risk-ingangen ([00-attack-surface-mapping](../pentest/00-attack-surface-mapping.md)) | — | ✅ | — |
| 10 | Beperken impact van incidenten (mitigatietechnieken) | Canonical-path-check, input-validatie, log-injection-preventie ([HFE-01/04 ná](../pentest/README.md)) | A.8.8 | ✅ | — |
| 11 | Security-info via logging/monitoring | Audit-logging `FormEntryAuditLogFormatter` ([08-logging](../08-logging.md)) | A.8.15 | ⚠️ | Geen retentie/rotatie + log-integriteit ([gap A.8.15](01-gap-analyse.md)) |
| 12 | Veilige updates (verhelpen kwetsbaarheden via update) | Dependabot + CI/CD-release ([dependabot.yml](../../.github/dependabot.yml), [otap](../otap.md)) | A.8.8 | ✅ | — |

## 2. Annex I — Deel II: Vulnerability-handling-eisen

| # | CRA-eis| bewijs | NEN-7510 | Status | Gap |
|---|---------------------------|---------------------------|----------|--------|-----|
| 1 | Componenten + kwetsbaarheden identificeren/documenteren (SBOM) | SPDX ([deploy.yml](../../.github/workflows/deploy.yml)) + CycloneDX ([snyk.yml](../../.github/workflows/snyk.yml)); [backlog](06-security-backlog.md) | A.8.8 | ✅ | — |
| 2 | Kwetsbaarheden onverwijld verhelpen | Geprioriteerd patchadvies (CVSS) ([07-patchadvies](07-patchadvies.md)) | A.8.8 | ⚠️ | Patches nog niet doorgevoerd |
| 3 | Regelmatige tests en reviews | Snyk SAST/SCA, [zizmor](../zizmor.md), pentest, [code-review](../review-pr15.md) | A.8.8 | ✅ | — |
| 4 | Info over verholpen kwetsbaarheden publiceren | [Traceability-matrix](10-traceability-matrix.md) (voor/na); release-notes-toezegging in [security.md](../security.md) | — | ⚠️ | Geen publieke advisory bij deze findings |
| 5 | Coordinated Vulnerability Disclosure (CVD) beleid | [security.md](../security.md) — meldproces, reactietermijnen, accept/afwijs | — | ✅ | — |
| 6 | Contactadres / kanaal voor kwetsbaarheidsmeldingen | `security@openmrs.org` ([security.md](../security.md)) | — | ✅ | — |
| 7 | Veilige distributie van updates | CI/CD-release via OTAP ([otap](../otap.md), [pipeline-compliance](02-pipeline-compliance.md)) | — | ⚠️ | Artefact-signing nog niet aangetoond |
| 8 | Patches onverwijld + gratis verspreiden met advisory | Dependabot-PR's ([dependabot.yml](../../.github/dependabot.yml)); disclosure-toezegging ([security.md](../security.md)) | A.8.8 | ⚠️ | Advisory-tekst per fix ontbreekt |

---

## 3. Samenvatting & advies

- **Sterk gedekt:** dataminimalisatie , integriteit , aanvalsoppervlak ,
  veilige updates , SBOM , tests/reviews  en — sinds [`security.md`](../security.md) —
  het volledige **CVD-beleid + meldkanaal** .
- **Resterende gaten:** publieke advisory's per verholpen kwetsbaarheid en
  **artefact-signing** bij distributie ; op productniveau CSRF-by-default
  en logretentie/-integriteit .

