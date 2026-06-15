# SonarCloud — setup en secrets

Handmatige stappen vóór de CI-workflow werkt. Uitvoeren door een teamlid met SonarCloud- en GitHub-adminrechten.

## A1. Organization en project key

1. Ga naar [sonarcloud.io](https://sonarcloud.io) en log in.
2. Open het project `openmrs-module-htmlformentry`.
3. **Project Settings** → **General Settings**.
4. Noteer:

| Veld | Waarde in repo |
|------|----------------|
| Organization | `bami-flogert` (in [`sonar-project.properties`](../sonar-project.properties)) |
| Project Key | `bami-flogert_openmrs-module-htmlformentry` |

Wijkt de project key in SonarCloud af? Pas `sonar-project.properties` aan vóór merge.

## A2. SONAR_TOKEN aanmaken

1. SonarCloud → profiel-avatar → **My Account** → **Security**.
2. **Generate Tokens**:
   - Name: `github-actions-htmlformentry`
   - Type: Global Analysis Token of Project Analysis Token
   - Expires: na einde LU2 (bijv. 3–6 maanden)
3. Kopieer de token direct (eenmalig zichtbaar).

## A3. GitHub Secret

1. [Repository Settings → Secrets → Actions](https://github.com/bami-flogert/openmrs-module-htmlformentry/settings/secrets/actions)
2. **New repository secret**
3. Name: `SONAR_TOKEN` (exact)
4. Value: token uit A2

## A4. GitHub-integratie (PR-decoratie)

1. SonarCloud → Organization Settings → **GitHub**.
2. Controleer dat `bami-flogert/openmrs-module-htmlformentry` gekoppeld is.
3. Project Settings → **Pull Request Decoration** → GitHub ingeschakeld.

## A5. Quality Gate (NFR-M6)

SonarCloud → **Quality Gates** → default **Sonar way** (free plan: geen custom gates).

New Code policy (minimum):

| Drempel | Instelling |
|---------|------------|
| New bugs | 0 blocker/critical |
| New code smells | 0 critical |
| S3776 | 0 new violations on new code |
| Duplication | Geen verslechtering |

## A6. Branch protection (na eerste groene run)

**Settings → Branches** → rule op `acceptatie` (en later `main`):

- Required status check: **SonarCloud Analysis**

**Let op:** Zonder `SONAR_TOKEN` faalt de Maven-job met auth-fout. De aparte GitHub App-check "SonarCloud Code Analysis" is niet hetzelfde als deze workflow-job.

De Sonar-scan draait op **JDK 17** (scanner-vereiste); build en tests blijven op **JDK 8** (OpenMRS-module).

## Checklist

- [ ] Organization en project key gecontroleerd
- [ ] `SONAR_TOKEN` in GitHub Actions secrets
- [ ] GitHub-integratie actief
- [ ] Eerste PR-run groen (Quality Gate Passed)
- [ ] Branch protection met SonarCloud-check

## Lokaal testen (optioneel)

```bash
# Windows PowerShell
$env:SONAR_TOKEN="<token>"
mvn -B package -DskipTests
mvn -B -pl omod test verify
mvn -B sonar:sonar -Dsonar.qualitygate.wait=true
```
