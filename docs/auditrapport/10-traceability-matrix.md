# Traceability matrix (bijlage E)

**Module:** OpenMRS HTML Form Entry v3.10.0
**Datum:** 2026-06-17
**Doel:** elke bevinding herleidbaar koppelen aan de geraakte **NEN-7510-control**, het
**risico**, het **bewijs/de test** en de **mitigatie + validatie**. Zo is in één
overzicht zichtbaar dat elke kwetsbaarheid is onderbouwd én afgehandeld.

---

## 1. Aangetoonde kwetsbaarheden

| ID | Kwetsbaarheid (CWE) | NEN-control | Risico (bow-tie) | Test / bewijs | Mitigatie (code) | Validatie (hertest) | Status |
|----|---------------------|-------------|------------------|---------------|------------------|---------------------|--------|
| **HFE-01** | Arbitrary file read / path traversal (CWE-22/73/200) | A.8.3 · A.8.5 | D2 datalek (C=5, score 20) | [voor](../pentest/bevinding-hfe-01-voor.md) · [bewijs](../pentest/bewijs/) (`hfe01-*.json/png`) | `resolveSafePreviewFile()` — whitelist-basismap + canonical-path-check | [na ✅](../pentest/bevinding-hfe-01-na.md) (N1–N4) | Opgelost |
| **HFE-02** | Missing authz + CSRF + IDOR (CWE-862/352/639) | A.8.3 · A.8.5 | Datamanipulatie (I=5) | [voor](../pentest/bevinding-hfe-02-voor.md) · [bewijs](../pentest/bewijs/) (`hfe02-*`) | `requirePrivilege("Delete Encounters")` + `isSameOrigin()` + fail-closed | [na ✅](../pentest/bevinding-hfe-02-na.md) (A2 + positieve controle) | Opgelost |
| **HFE-03** | Open redirect (CWE-601) | A.8.5 | D2 datalek/phishing | [voor](../pentest/bevinding-hfe-03-voor.md) · [bewijs](../pentest/bewijs/) (`hfe03-*`) | `isSafeRelativeUrl()` — alleen interne relatieve paden | [na ✅](../pentest/bevinding-hfe-03-na.md) (A3) | Opgelost |

## 1a. Geïdentificeerd, (nog) niet aangetoond of gemitigeerd

Bevinding uit code-review die buiten de pentest-selectie van §1 viel — wel
geïdentificeerd en expliciet vastgelegd, maar nog niet met een live exploit
aangetoond en nog niet in de modulecode gefixt.

| ID | Kwetsbaarheid (CWE) | NEN-control | Risico | Bron | Aanbevolen vervolg | Status |
|----|---------------------|-------------|--------|------|---------------------|--------|
| **HFE-009** | Onveilige deserialisatie in `loadSession.form` (eigen omod-code, geen CVE) — CWE-502 | A.8.3 · A.8.8 | RCE-potentieel (i.c.m. gadget-chain HFE-005) | [attack-surface-mapping E05](../pentest/00-attack-surface-mapping.md) · [backlog HFE-009](06-security-backlog.md#65-eigen-code-bevinding-buiten-snyk-scope-hfe-009) | Live exploit (volgende pentestronde) + allow-list/`ObjectInputFilter` of JSON i.p.v. native serialisatie | **Open — niet gemitigeerd** |

## 2. Kwetsbare afhankelijkheden

| ID | Component / CVE | NEN-control | Contextuele score | Bron | Advies | Status |
|----|-----------------|-------------|-------------------|------|--------|--------|
| **HFE-001** | log4j 1.2.15 — CVE-2019-17571 | A.8.8 · A.8.15 | 8.8 (H) | [backlog](06-security-backlog.md) | [patchadvies](07-patchadvies.md) | Open |
| **HFE-002** | commons-fileupload 1.2.1 — CVE-2016-1000031 | A.8.8 | 8.5 (H) | [backlog](06-security-backlog.md) | [patchadvies](07-patchadvies.md) | Open |
| **HFE-003** | jQuery 1.8.3 / UI 1.9.2 (gebundeld) — CVE-2020-11023 e.a. | A.8.28 | 8.0 (H) | [backlog](06-security-backlog.md) | upgrade in module (Sprint 3) | Open |
| **HFE-004** | spring-beans 3.0.5 — CVE-2022-22965 (Spring4Shell) | A.8.8 | 7.0 (H) | [backlog](06-security-backlog.md) | [patchadvies](07-patchadvies.md) | Open |
| **HFE-005** | commons-collections 3.2 — CVE-2015-7501 | A.8.8 | 6.5 (M) | [backlog](06-security-backlog.md) | [patchadvies](07-patchadvies.md) | Open |
| **HFE-006** | groovy 1.8.3 — CVE-2015-3253 | A.8.8 · A.8.28 | 5.5 (M) | [backlog](06-security-backlog.md) | scope → `test` (in module) | Open |
| **HFE-007** | handlebars 1.0.12 (gebundeld) — CVE-2019-19919 | A.8.28 | 5.5 (M) | [backlog](06-security-backlog.md) | upgrade met HFE-003 | Open |
| **HFE-008** | commons-codec 1.10 (test-classpath) | A.8.8 | n.v.t. | [backlog](06-security-backlog.md) | — | False positive |

## 3. Compliance-gaps → control → verbetering

| Control | Gap | Geraakt risico | Verbetering | Bron |
|---------|-----|----------------|-------------|------|
| **A.8.3** Toegangsbeveiliging | Geen `@Authorized` op service-laag; geen privilege-matrix | D5 onbevoegde inzage | `@Authorized` op schrijfmethoden + privilege-matrix documenteren | [gap A.8.3](01-gap-analyse.md#