#!/usr/bin/env python3
"""Generate one end-to-end report per proof of concept.

Inputs
    --cucumber-json   Cucumber JSON report (carries the @PoC-* tags and scenario results)
    --jacoco-csv      JaCoCo CSV coverage report
    --metrics         optional latency CSV recorded by the B1 timing scenario
    --out-dir         directory to write the reports into

Outputs
    <out-dir>/poc-coverage-matrix.md   one row per proof of concept
    <out-dir>/<PoC>.md                 the scenarios and coverage for a single proof of concept

The generator never fails the build on missing data; it reports what it could not find so the
gap is visible instead of silent.
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import sys
from collections import defaultdict

# Which application modules each proof of concept is expected to exercise.
POC_MODULES = {
    "A1": ["party", "relationship", "contactpoint", "onboarding"],
    "A2": ["party", "productholding"],
    "B1": ["party", "relationship", "contactpoint", "productholding", "onboarding"],
    "B2": ["party", "relationship", "contactpoint", "productholding", "onboarding"],
    "B3": ["party", "productholding", "onboarding"],
    "B4": ["party", "productholding", "onboarding"],
    "R1": ["party", "relationship", "contactpoint", "productholding", "onboarding"],
    "M": ["onboarding"],
    "J": ["party", "relationship", "contactpoint", "productholding", "onboarding"],
    "MIG": ["migration", "party", "relationship", "contactpoint", "productholding"],
    "RM": ["readmodel", "party", "relationship", "contactpoint", "productholding"],
}

POC_TITLES = {
    "A1": "Region-local onboarding in the UK",
    "A2": "Region-local product holding in Hong Kong",
    "B1": "Cross-region onboarding",
    "B2": "Cross-region rollback",
    "B3": "Concurrent onboarding (serializable isolation)",
    "B4": "Region quorum loss and recovery",
    "R1": "Data residency",
    "M": "No compensation machinery",
    "J": "Full front-to-back journey",
    "MIG": "MongoDB to Distributed SQL migration",
    "RM": "Eventually consistent customer-360 read model",
}


BASE_PACKAGE = "io.forest.cdm"


def module_of(package: str) -> str:
    """io.forest.cdm.party.internal -> party"""
    prefix = BASE_PACKAGE + "."
    if not package.startswith(prefix):
        return "(root)"
    return package[len(prefix):].split(".")[0]


def read_coverage(path: str) -> dict:
    """Return {module: {"line": [covered, total], "branch": [covered, total]}}."""
    modules: dict = defaultdict(lambda: {"line": [0, 0], "branch": [0, 0]})
    if not os.path.exists(path):
        return {}
    with open(path, newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            module = module_of(row["PACKAGE"])
            modules[module]["line"][0] += int(row["LINE_COVERED"])
            modules[module]["line"][1] += int(row["LINE_MISSED"]) + int(row["LINE_COVERED"])
            modules[module]["branch"][0] += int(row["BRANCH_COVERED"])
            modules[module]["branch"][1] += int(row["BRANCH_MISSED"]) + int(row["BRANCH_COVERED"])
    return dict(modules)


def read_scenarios(path: str) -> dict:
    """Group scenarios by their @PoC-* tag."""
    grouped: dict = defaultdict(list)
    if not os.path.exists(path):
        print(f"[poc-report] WARNING: no Cucumber report at {path}", file=sys.stderr)
        return {}
    with open(path, encoding="utf-8") as handle:
        features = json.load(handle)

    for feature in features:
        for element in feature.get("elements", []):
            if element.get("type") != "scenario":
                continue
            tags = [t["name"] for t in element.get("tags", [])]
            poc_tags = [t for t in tags if t.startswith("@PoC-")]
            if not poc_tags:
                continue
            steps = element.get("steps", [])
            statuses = [s.get("result", {}).get("status", "unknown") for s in steps]
            duration = sum(s.get("result", {}).get("duration", 0) for s in steps)
            if "failed" in statuses:
                status = "failed"
            elif "undefined" in statuses:
                status = "undefined"
            elif "skipped" in statuses:
                status = "skipped"
            else:
                status = "passed"
            entry = {
                "feature": feature.get("name", "?"),
                "scenario": element.get("name", "?"),
                "status": status,
                "duration_ms": duration / 1_000_000.0,
            }
            for tag in poc_tags:
                grouped[tag.replace("@PoC-", "")].append(entry)
    return dict(grouped)


def read_metrics(path: str) -> list:
    if not path or not os.path.exists(path):
        return []
    with open(path, newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def pct(covered: int, total: int) -> str:
    return "n/a" if total == 0 else f"{100.0 * covered / total:.1f}%"


def write_reports(scenarios: dict, coverage: dict, metrics: list, out_dir: str) -> int:
    os.makedirs(out_dir, exist_ok=True)
    poc_ids = sorted(set(scenarios) | set(POC_MODULES))
    failures = 0
    matrix = [
        "# PoC end-to-end coverage matrix",
        "",
        "Generated from the Cucumber JSON report and the JaCoCo CSV report.",
        "Scenario coverage is the pass/fail gate; code coverage shows how much of each module the",
        "end-to-end scenarios actually exercised.",
        "",
        "| PoC | Description | Scenarios | Passed | Failed | Modules | Line coverage |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]

    for poc in poc_ids:
        entries = scenarios.get(poc, [])
        passed = sum(1 for e in entries if e["status"] == "passed")
        failed = sum(1 for e in entries if e["status"] in {"failed", "undefined"})
        failures += failed
        modules = POC_MODULES.get(poc, [])
        cov_covered = sum(coverage.get(m, {}).get("line", [0, 0])[0] for m in modules)
        cov_total = sum(coverage.get(m, {}).get("line", [0, 0])[1] for m in modules)

        matrix.append(
            f"| {poc} | {POC_TITLES.get(poc, '')} | {len(entries)} | {passed} | {failed} | "
            f"{', '.join(modules)} | {pct(cov_covered, cov_total)} |"
        )

        lines = [
            f"# PoC {poc} - {POC_TITLES.get(poc, '')}",
            "",
            f"- Scenarios: {len(entries)} (passed {passed}, failed {failed})",
            f"- Modules exercised: {', '.join(modules)}",
            f"- Line coverage across those modules: {pct(cov_covered, cov_total)}",
            "",
            "## Scenarios",
            "",
            "| Result | Feature | Scenario | Duration (ms) |",
            "| --- | --- | --- | --- |",
        ]
        for entry in entries:
            lines.append(
                f"| {entry['status']} | {entry['feature']} | {entry['scenario']} | "
                f"{entry['duration_ms']:.0f} |"
            )
        if not entries:
            lines.append("| - | - | no scenarios were reported for this PoC | - |")

        if poc == "B1" and metrics:
            lines += [
                "",
                "## Recorded latency (single-host cluster)",
                "",
                "| Pattern | Elapsed (ms) |",
                "| --- | --- |",
            ]
            for row in metrics:
                lines.append(f"| {row.get('pattern', '?')} | {row.get('elapsed_millis', '?')} |")
            lines += [
                "",
                "> A single-host cluster has no real inter-region hop, so these numbers are",
                "> evidence of behaviour, not a latency budget.",
            ]

        lines += ["", "## Module coverage", "", "| Module | Line | Branch |", "| --- | --- | --- |"]
        for module in modules:
            data = coverage.get(module)
            if not data:
                lines.append(f"| {module} | n/a | n/a |")
            else:
                lines.append(
                    f"| {module} | {pct(data['line'][0], data['line'][1])} | "
                    f"{pct(data['branch'][0], data['branch'][1])} |"
                )

        with open(os.path.join(out_dir, f"poc-{poc}.md"), "w", encoding="utf-8") as handle:
            handle.write("\n".join(lines) + "\n")

    with open(os.path.join(out_dir, "poc-coverage-matrix.md"), "w", encoding="utf-8") as handle:
        handle.write("\n".join(matrix) + "\n")

    print(f"[poc-report] wrote {len(poc_ids)} PoC reports to {out_dir}")
    if failures:
        print(f"[poc-report] {failures} scenario(s) did not pass", file=sys.stderr)
    return failures


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cucumber-json", required=True)
    parser.add_argument("--jacoco-csv", required=True)
    parser.add_argument("--metrics", default="target/e2e-metrics/latency.csv")
    parser.add_argument("--out-dir", required=True)
    args = parser.parse_args()

    write_reports(
        read_scenarios(args.cucumber_json),
        read_coverage(args.jacoco_csv),
        read_metrics(args.metrics),
        args.out_dir,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
