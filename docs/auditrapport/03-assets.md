# 3. Asset-identificatie

## 3.1 Aanpak

Voor de NEN 7510-analyse van de OpenMRS HTML Form Entry-module (v3.10.0) is in kaart gebracht welke kroonjuwelen de module verwerkt. De module vormt de formulierservice waarmee zorgverleners klinische gegevens invoeren en raadplegen; alle onderstaande assets passeren of ontstaan in deze module. Per asset is een BIV-classificatie toegekend op een schaal van 1 t/m 5, met de toelichting.

## 3.2 Kroonjuwelen

| Kroonjuweel | Omschrijving | Klasse | Toelichting                           | BIV |
|---|---|---|---------------------------------------|---|
| **Klinische obs-data** | Vitale waarden, diagnoses, lab-uitslagen | Bijzondere persoonsgegevens | AVG art. 9 lid 1; NEN 7510-1 §A.8     | C=5 I=5 A=4 |
| **Medicatieorders** | Dosering, geneesmiddel | Bijzondere persoonsgegevens | AVG art. 9; Wet BIG; NEN 7510-2 §A.14 | C=5 I=5 A=5 |
| **Patiëntidentiteit** | NAW-gegevens, BSN, geboortedatum | Bijzondere persoonsgegevens | AVG art. 4(1); NEN 7510-1 §A.8.2      | C=5 I=4 A=3 |
| **Audit-logbestanden** | Formulierinzendingen, wijzigingshistorie, gebruikersacties | Vertrouwelijk | NEN 7510-2 §A.12.4; WGBO art. 7:454   | C=3 I=5 A=4 |
| **Zorgproces­continuïteit** | Beschikbaarheid formulierservice voor klinische workflow | Operationeel kritiek | NEN 7510-1 §6.1; BIO maatregel 17.1   | C=1 I=3 A=5 |

## 3.3 Toelichting op de classificatie

**Medicatieorders** scoren maximaal op alle drie de aspecten: een gelekte order schendt het medisch beroepsgeheim, een gemanipuleerde dosering kan directe patiëntschade veroorzaken en zonder actuele orders vertraagd de medicatieverstrekking.

**Klinische obs-data en patiëntidentiteit** zijn bijzondere persoonsgegevens in de zin van AVG; verwerking is alleen toegestaan onder de uitzonderingsgrond voor gezondheidszorg en vereist passende technische en organisatorische maatregelen. De combinatie van BSN met medische gegevens maakt patiëntidentiteit bovendien aantrekkelijk voor identiteitsfraude.

**Audit-logbestanden** zijn vooral integriteitskritisch: zij vormen het bewijsmiddel voor de verantwoordingsplicht en voor detectie van onbevoegde inzage. De vertrouwelijkheidseis is lager omdat de logs zelf beperkte medische inhoud bevatten.

**Zorgproces­continuïteit** is geen gegevensverzameling maar een operationele asset: de formulierservice ondersteunt de klinische workflow direct, waardoor beschikbaarheid het belangrijke aspect is.

