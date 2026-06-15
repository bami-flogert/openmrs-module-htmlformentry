#!/usr/bin/env python3
"""Generate prioritized patch advice from Snyk SCA JSON output."""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import date
from pathlib import Path
from typing import Any

MODULE_VERSION = "3.10.0"
SEVERITY_ORDER = {"critical": 0, "high": 1, "medium": 2, "low": 3}

NON_PATCHABLE = [
    {
        "component": "org.openmrs.api:*",
        "reason": "OpenMRS-platformversie (provided scope); beheerd via Maven-profiel, genegeerd door Dependabot",
        "current": "1.9.9 (default profiel)",
    },
    {
        "component": "org.openmrs.web:*",
        "reason": "OpenMRS-platformversie (provided scope); upgrade vereist platformmigratie",
        "current": "1.9.9 (default profiel)",
    },
]


@dataclass
class Vulnerability:
    component: str
    current_version: str
    target_version: str
    cves: list[str]
    cvss: float
    severity: str
    title: str
    source: str
    ecosystem: str = "maven"

    @property
    def key(self) -> str:
        return f"{self.ecosystem}:{self.component}@{self.current_version}"


@dataclass
class PatchGroup:
    component: str
    current_version: str
    target_version: str
    ecosystem: str
    cves: set[str] = field(default_factory=set)
    max_cvss: float = 0.0
    severity: str = "low"
    titles: list[str] = field(default_factory=list)
    sources: set[str] = field(default_factory=set)

    def absorb(self, vuln: Vulnerability) -> None:
        self.cves.update(vuln.cves)
        self.max_cvss = max(self.max_cvss, vuln.cvss)
        if SEVERITY_ORDER.get(vuln.severity, 99) < SEVERITY_ORDER.get(self.severity, 99):
            self.severity = vuln.severity
        if vuln.title and vuln.title not in self.titles:
            self.titles.append(vuln.title)
        self.sources.add(vuln.source)
        if vuln.target_version and vuln.target_version != self.current_version:
            self.target_version = vuln.target_version


def parse_cvss(value: Any) -> float:
    if value is None:
        return 0.0
    if isinstance(value, (int, float)):
        return float(value)
    if isinstance(value, str):
        match = re.search(r"(\d+(?:\.\d+)?)", value)
        if match:
            return float(match.group(1))
    return 0.0


def extract_target_version(upgrade_path: list[Any]) -> str:
    for item in reversed(upgrade_path):
        if isinstance(item, str) and item and item.lower() not in {"false", "true"}:
            if "@" in item:
                return item.split("@", 1)[1]
            if ":" in item and not item.startswith("http"):
                return item.split(":", 1)[1]
            if re.match(r"^v?\d", item):
                return item.lstrip("v")
            return item
    return ""


def component_label(package_name: str, version: str, name: str | None = None) -> str:
    if package_name:
        return package_name
    if name and "@" in name:
        return name.split("@", 1)[0]
    return name or "unknown"


def parse_vulnerability(raw: dict[str, Any], source: str, ecosystem: str) -> Vulnerability | None:
    package_name = raw.get("packageName") or ""
    version = raw.get("version") or ""
    name = raw.get("name") or ""
    component = component_label(package_name, version, name)

    if not component or component == "unknown":
        return None

    identifiers = raw.get("identifiers") or {}
    cves = identifiers.get("CVE") or []
    if not cves and raw.get("id", "").startswith("CVE-"):
        cves = [raw["id"]]

    cvss = parse_cvss(raw.get("CVSSv3") or raw.get("cvssScore"))
    severity = (raw.get("severity") or "low").lower()
    target = extract_target_version(raw.get("upgradePath") or [])
    if not target:
        target = raw.get("fixedIn") or raw.get("nearestFixedInVersion") or ""

    return Vulnerability(
        component=component,
        current_version=version,
        target_version=target,
        cves=cves,
        cvss=cvss,
        severity=severity,
        title=(raw.get("title") or "").strip(),
        source=source,
        ecosystem=ecosystem,
    )


def load_snyk_documents(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        return []

    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return []

    if isinstance(payload, list):
        return [doc for doc in payload if isinstance(doc, dict)]
    if isinstance(payload, dict):
        return [payload]
    return []


def ecosystem_for_document(doc: dict[str, Any], default: str) -> str:
    manager = (doc.get("packageManager") or "").lower()
    if "docker" in manager or "container" in manager:
        return "docker"
    if "maven" in manager or "gradle" in manager:
        return "maven"
    return default


def collect_vulnerabilities(
    paths: list[Path],
    default_ecosystem: str,
) -> list[Vulnerability]:
    findings: list[Vulnerability] = []

    for path in paths:
        for doc in load_snyk_documents(path):
            ecosystem = ecosystem_for_document(doc, default_ecosystem)
            for raw in doc.get("vulnerabilities") or []:
                if not isinstance(raw, dict):
                    continue
                vuln = parse_vulnerability(raw, source=path.name, ecosystem=ecosystem)
                if vuln:
                    findings.append(vuln)

    return findings


def group_findings(findings: list[Vulnerability]) -> list[PatchGroup]:
    groups: dict[str, PatchGroup] = {}

    for vuln in findings:
        key = vuln.key
        if key not in groups:
            groups[key] = PatchGroup(
                component=vuln.component,
                current_version=vuln.current_version,
                target_version=vuln.target_version,
                ecosystem=vuln.ecosystem,
            )
        groups[key].absorb(vuln)

    ordered = sorted(
        groups.values(),
        key=lambda g: (
            -g.max_cvss,
            SEVERITY_ORDER.get(g.severity, 99),
            -len(g.cves),
            g.component.lower(),
        ),
    )
    return ordered


def severity_counts(findings: list[Vulnerability]) -> dict[str, int]:
    counts: dict[str, int] = defaultdict(int)
    for vuln in findings:
        counts[vuln.severity] += 1
    return dict(counts)


def risk_reduction_rows(groups: list[PatchGroup]) -> list[tuple[int, str, float, float, str]]:
    baseline = sum(min(g.max_cvss, 10.0) for g in groups)
    remaining = baseline
    rows: list[tuple[int, str, float, float, str]] = []

    for index, group in enumerate(groups, start=1):
        delta = min(group.max_cvss, 10.0)
        remaining = max(0.0, remaining - delta)
        reduction = 0.0 if baseline == 0 else ((baseline - remaining) / baseline) * 100
        label = f"{group.component} → {group.target_version or 'zie advies'}"
        rows.append((index, label, delta, reduction, group.severity))

    return rows


def priority_reason(group: PatchGroup, rank: int) -> str:
    parts = [f"P{rank}: max CVSS {group.max_cvss:.1f} ({group.severity})"]
    if group.ecosystem == "docker":
        parts.append("infrastructuur/EOL-risico")
    if len(group.cves) > 1:
        parts.append(f"{len(group.cves)} CVE's")
    if group.target_version:
        parts.append("upgrade-pad beschikbaar")
    else:
        parts.append("geen automatisch upgrade-pad")
    return "; ".join(parts)


def format_cves(cves: set[str]) -> str:
    if not cves:
        return "—"
    return ", ".join(sorted(cves))


def has_scan_data(findings: list[Vulnerability]) -> bool:
    return len(findings) > 0


def render_markdown(
    maven_groups: list[PatchGroup],
    docker_groups: list[PatchGroup],
    maven_findings: list[Vulnerability],
    docker_findings: list[Vulnerability],
    maven_paths: list[Path],
    docker_paths: list[Path],
    output: Path,
    status_note: str = "",
) -> str:
    today = date.today().isoformat()
    all_findings = maven_findings + docker_findings
    scan_available = has_scan_data(all_findings)
    status = "Gegenereerd uit Snyk-scan" if scan_available else "DRAFT — geen scan-data (SNYK_TOKEN vereist)"
    if status_note:
        status = f"{status} — {status_note}"

    counts = severity_counts(all_findings)
    maven_rows = risk_reduction_rows(maven_groups)
    docker_rows = risk_reduction_rows(docker_groups)
    combined_groups = sorted(
        maven_groups + docker_groups,
        key=lambda g: (
            -g.max_cvss,
            SEVERITY_ORDER.get(g.severity, 99),
            -len(g.cves),
            g.component.lower(),
        ),
    )
    combined_rows = risk_reduction_rows(combined_groups)
    baseline = sum(min(g.max_cvss, 10.0) for g in combined_groups)

    lines: list[str] = [
        "# Patchadvies — SBOM & CVE/CVSS",
        "",
        f"**Module:** OpenMRS HTML Form Entry v{MODULE_VERSION}  ",
        f"**Datum:** {today}  ",
        f"**Status:** {status}  ",
        "**Bronnen:** SPDX SBOM (`deploy.yml`), CycloneDX SBOM (`snyk sbom`), Snyk SCA (Maven + Docker)",
        "",
        "---",
        "",
        "## 1. Samenvatting",
        "",
    ]

    if scan_available:
        lines.extend(
            [
                f"- **Totaal kwetsbaarheden:** {len(all_findings)} ({len(combined_groups)} unieke componenten)",
                f"- **Critical:** {counts.get('critical', 0)} | **High:** {counts.get('high', 0)} | "
                f"**Medium:** {counts.get('medium', 0)} | **Low:** {counts.get('low', 0)}",
                f"- **Baseline risk score:** {baseline:.1f} (som max-CVSS per component, cap 10)",
                "",
            ]
        )
    else:
        lines.extend(
            [
                "Er is geen geldige Snyk JSON-input gevonden. Voer de workflow [`snyk.yml`](../../.github/workflows/snyk.yml) uit "
                "met `SNYK_TOKEN` of genereer lokaal:",
                "",
                "```bash",
                "mvn -B install -DskipTests",
                "snyk test --all-projects --json-file-output=snyk-results.json",
                "snyk container test mysql:5.6.51 --json-file-output=snyk-container-mysql.json",
                "snyk container test openmrs/openmrs-reference-application-distro:2.12.2 --json-file-output=snyk-container-openmrs.json",
                "python3 .github/scripts/generate-patch-advice.py \\",
                "  --maven snyk-results.json \\",
                "  --docker snyk-container-mysql.json snyk-container-openmrs.json \\",
                "  --output docs/auditrapport/07-patchadvies.md",
                "```",
                "",
                "Onderstaande structuur is het verwachte rapportformaat zodra scan-data beschikbaar is.",
                "",
            ]
        )

    lines.extend(
        [
            "## 2. Geprioriteerde patchvolgorde (Maven)",
            "",
            "| # | Component | Huidig | Advies | CVE(s) | CVSS | Reden prioriteit |",
            "|---|-----------|--------|--------|--------|------|------------------|",
        ]
    )

    if maven_groups:
        for rank, group in enumerate(maven_groups, start=1):
            lines.append(
                f"| {rank} | `{group.component}` | {group.current_version or '—'} | "
                f"{group.target_version or '—'} | {format_cves(group.cves)} | "
                f"{group.max_cvss:.1f} | {priority_reason(group, rank)} |"
            )
    else:
        lines.append("| — | _Geen Maven-data_ | — | — | — | — | — |")

    lines.extend(
        [
            "",
            "## 3. Docker / infrastructuur",
            "",
            "Images uit [`docker-compose.yml`](../../docker-compose.yml):",
            "",
            "| # | Image | Huidig | Advies | CVE(s) | CVSS | Reden prioriteit |",
            "|---|-------|--------|--------|--------|------|------------------|",
        ]
    )

    if docker_groups:
        for rank, group in enumerate(docker_groups, start=1):
            lines.append(
                f"| {rank} | `{group.component}` | {group.current_version or '—'} | "
                f"{group.target_version or 'upgrade image-tag'} | {format_cves(group.cves)} | "
                f"{group.max_cvss:.1f} | {priority_reason(group, rank)} |"
            )
    else:
        lines.extend(
            [
                "| 1 | `mysql` | 5.6.51 | 8.0 LTS (compose-overlays) | _scan vereist_ | — | EOL-database; bekend hoog infrastructuurrisico |",
                "| 2 | `openmrs-reference-application-distro` | 2.12.2 | nieuwste compatibele distro-tag | _scan vereist_ | — | container-base-image updates |",
            ]
        )

    lines.extend(
        [
            "",
            "## 4. Verwachte risicoreductie",
            "",
            "Model: per component wordt de hoogste CVSS-score (max 10) opgeteld als baseline. "
            "Bij elke patch wordt die score afgetrokken; cumulatief percentage = `(baseline − resterend) / baseline`.",
            "",
            "> Schatting op basis van Snyk upgrade paths. Regressietests (`mvn test`, smoke test) zijn vereist na elke upgrade.",
            "",
            "### Gecombineerde volgorde",
            "",
            "| Stap | Actie | CVSS-oplossing | Cumulatieve reductie | Ernst |",
            "|------|-------|----------------|----------------------|-------|",
        ]
    )

    if combined_rows:
        for step, label, delta, reduction, severity in combined_rows:
            lines.append(
                f"| {step} | {label} | {delta:.1f} | {reduction:.1f}% | {severity} |"
            )
        final_reduction = combined_rows[-1][3] if combined_rows else 0.0
        lines.append("")
        lines.append(
            f"**Verwachte totale risicoreductie na alle stappen:** {final_reduction:.1f}% "
            f"(baseline {baseline:.1f} → resterend {max(0.0, baseline - sum(r[2] for r in combined_rows)):.1f})"
        )
    else:
        lines.append("| — | _Geen data_ | — | — | — |")

    if maven_rows and docker_rows:
        lines.extend(
            [
                "",
                "### Alleen Maven",
                "",
                "| Stap | Actie | CVSS-oplossing | Cumulatieve reductie |",
                "|------|-------|----------------|----------------------|",
            ]
        )
        for step, label, delta, reduction, _ in maven_rows:
            lines.append(f"| {step} | {label} | {delta:.1f} | {reduction:.1f}% |")

    lines.extend(
        [
            "",
            "## 5. Niet-patchbare items (module-scope)",
            "",
            "| Component | Huidige situatie | Reden |",
            "|-----------|------------------|-------|",
        ]
    )
    for item in NON_PATCHABLE:
        lines.append(f"| `{item['component']}` | {item['current']} | {item['reason']} |")

    lines.extend(
        [
            "",
            "## 6. Inputbestanden",
            "",
            "| Bron | Bestand | Aanwezig |",
            "|------|---------|----------|",
        ]
    )

    display_paths = [
        ("Maven SCA", maven_paths, "snyk-results.json"),
        (
            "Docker SCA",
            docker_paths,
            ["snyk-container-mysql.json", "snyk-container-openmrs.json"],
        ),
    ]
    for label, paths, defaults in display_paths:
        entries = paths if paths else [Path(p) for p in (defaults if isinstance(defaults, list) else [defaults])]
        for path in entries:
            display = path.name if path.is_absolute() else str(path)
            present = "✅" if path.is_file() else "❌"
            lines.append(f"| {label} | `{display}` | {present} |")

    lines.extend(
        [
            "",
            "## 7. Verwijzingen",
            "",
            "- SPDX SBOM: [`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml) → `docs/sbom.spdx.json`",
            "- CycloneDX SBOM: [`.github/workflows/snyk.yml`](../../.github/workflows/snyk.yml) → `snyk-sbom.json`",
            "- OTAP-pipeline: [`docs/otap.md`](../otap.md)",
            "- Hoofdrapport: [`00-auditrapport.md`](00-auditrapport.md)",
            "",
            "---",
            "",
            "_Gegenereerd door [`.github/scripts/generate-patch-advice.py`](../../.github/scripts/generate-patch-advice.py)._",
            "",
        ]
    )

    content = "\n".join(lines)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(content, encoding="utf-8")
    return content


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate patch advice markdown from Snyk JSON.")
    parser.add_argument("--maven", nargs="*", default=[], help="Maven Snyk JSON file(s)")
    parser.add_argument("--docker", nargs="*", default=[], help="Docker Snyk JSON file(s)")
    parser.add_argument(
        "--output",
        default="docs/auditrapport/07-patchadvies.md",
        help="Output markdown path",
    )
    parser.add_argument(
        "--status-note",
        default="",
        help="Optional extra status line for the report header",
    )
    args = parser.parse_args()

    maven_paths = [Path(p) for p in args.maven]
    docker_paths = [Path(p) for p in args.docker]
    output = Path(args.output)

    maven_findings = collect_vulnerabilities(maven_paths, "maven")
    docker_findings = collect_vulnerabilities(docker_paths, "docker")
    maven_groups = group_findings(maven_findings)
    docker_groups = group_findings(docker_findings)

    render_markdown(
        maven_groups=maven_groups,
        docker_groups=docker_groups,
        maven_findings=maven_findings,
        docker_findings=docker_findings,
        maven_paths=maven_paths or [Path("snyk-results.json")],
        docker_paths=docker_paths
        or [Path("snyk-container-mysql.json"), Path("snyk-container-openmrs.json")],
        output=output,
        status_note=args.status_note,
    )

    total = len(maven_findings) + len(docker_findings)
    print(f"Wrote {output} ({total} vulnerabilities from {len(maven_paths) + len(docker_paths)} input file(s))")
    return 0


if __name__ == "__main__":
    sys.exit(main())
