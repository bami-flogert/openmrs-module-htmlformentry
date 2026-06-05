<div align="center">

# Auditrapport

## Security & Compliance audit — OpenMRS HTML Form Entry

<br>

**Module:** OpenMRS HTML Form Entry (`openmrs-module-htmlformentry`) v3.10.0
**Normkader:** NEN-7510:2024-2 · AVG · EU Cyber Resilience Act (CRA)
**Scope:** Controls A.8.3 · A.8.5 · A.8.15 + CI/CD-pipeline

<br>

**Auteur / auditor:** frogissober
**Datum:** 2026-06-03
**Versie:** 0.1 — DRAFT (skelet)
**Status:** In bewerking 

</div>

---

## Inhoudsopgave

1. [Executive Summary](#1-executive-summary)
2. [Scope & Context](#2-scope--context)
3. [Audit Methodologie](#3-audit-methodologie)
4. [Risico-analyse & Bevindingen](#4-risico-analyse--bevindingen)
5. [SBOM & Supply Chain Security](#5-sbom--supply-chain-security)
6. [Conclusie & Advies](#6-conclusie--advies)
7. [Bijlagen](#7-bijlagen)

---

## 1. Executive Summary

> _In te vullen bij finalisatie._ Korte, niet-technische samenvatting: wat is onderzocht, het eindoordeel en de 2–3 belangrijkste risico's.

---

## 2. Scope & Context

> _In te vullen._ Geauditeerde module + versie, waarom deze module (verwerkt persoons-/medische gegevens → AVG), normkader (NEN-7510:2024-2, controls A.8.3/A.8.5/A.8.15) en de scope-grens (module-code wel, OpenMRS Core niet).

---

## 3. Audit Methodologie

> _In te vullen._ Aanpak en gebruikte tools: handmatige code-review, mapping op NEN-controls, statische analyse (SAST), SBOM-generatie, dependency-scan, risicoweging (kans × impact).

---

## 4. Risico-analyse & Bevindingen

> _In te vullen._ Per bevinding: beschrijving, bewijs (coderegel), geraakte control en risicoscore. Risico-gerichte samenvatting van de gap-analyse (zie bijlage A).

---

## 5. SBOM & Supply Chain Security

> _In te vullen._ Overzicht van afhankelijkheden (CycloneDX-SBOM), verouderde/kwetsbare componenten (o.a. MySQL 5.6, JDK 8) en bescherming van de keten. Sluit aan op het pipeline-verslag (bijlage B) en de SBOM (bijlage C).

---

## 6. Conclusie & Advies

> _In te vullen._ Eindoordeel + geprioriteerd advies. Verwijst door naar de security backlog (bijlage I).

---

## 7. Bijlagen

| # | Bijlage | Bestand | Status |
|---|---------|---------|--------|
| A | Gap-analyse | [`01-gap-analyse.md`](01-gap-analyse.md) | Aanwezig |
| B | Mini-complianceverslag pipeline | [`02-pipeline-compliance.md`](02-pipeline-compliance.md) | Aanwezig |
| C | SBOM (CycloneDX JSON) | _nog te genereren_ | Te doen |
| D | SAST-output (CodeQL / Snyk) | _nog te draaien_ | Te doen |
| E | Traceability matrix | _nog te maken_ | Te doen |
| F | Risicomatrix | _nog te maken_ | Te doen |
| G | Bow-tie / threat model | _nog te maken_ | Te doen |
| H | CRA-mapping | _nog te maken_ | Te doen |
| I | Security backlog / verbeteraanpak | _nog te maken_ | Te doen |

---

<div align="center">

### Ondertekening

_Wordt bij finalisatie als digitale handtekening op de PDF gezet._

| | |
|---|---|
| **Naam** | John Doe |
| **Rol** | Auditor |
| **Datum** | _________________ |
| **Handtekening** | _________________ |

</div>
