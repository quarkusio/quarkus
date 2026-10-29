#!/usr/bin/env python3
"""Keep integration test poms buildable against an already released Quarkus.

Dependencies on Quarkus artifacts that are *not* themselves built under
integration-tests/ must use ${quarkus.version} rather than ${project.version},
so that the integration tests can be built against a released Quarkus (or an
assembled platform BOM) without rebuilding Quarkus locally. See
integration-tests/README.md.

Artifacts that *are* built under integration-tests/ keep ${project.version}:
they are never published, so they are always built from the reactor whatever
Quarkus version is being tested against.

Usage:
    check-integration-test-versions.py          # report offenders, exit 1 if any
    check-integration-test-versions.py --fix    # rewrite them in place
"""

import os
import re
import sys
import xml.etree.ElementTree as ET

IT_ROOT = "integration-tests"
POM = "pom.xml"
MAVEN_NS = "{http://maven.apache.org/POM/4.0.0}"

OLD = "<version>${project.version}</version>"
NEW = "<version>${quarkus.version}</version>"

# Only <dependency> blocks are rewritten. A non-greedy match is safe because
# <dependency> elements cannot nest, so dependencies declared inside a <plugin>
# are matched correctly too.
DEPENDENCY = re.compile(r"<dependency>.*?</dependency>", re.DOTALL)
GROUP_ID = re.compile(r"<groupId>\s*([^<]+?)\s*</groupId>")
ARTIFACT_ID = re.compile(r"<artifactId>\s*([^<]+?)\s*</artifactId>")


def reactor_poms():
    """Every pom that is part of the integration test reactor.

    Anything under a src/ or target/ directory is skipped: src/ holds the
    template projects used by the Maven and devservices integration tests,
    which are filtered through @project.version@ and must not be touched.
    """
    found = []
    for dirpath, dirnames, filenames in os.walk(IT_ROOT):
        dirnames[:] = [d for d in dirnames if d not in ("src", "target")]
        if POM in filenames:
            found.append(os.path.join(dirpath, POM))
    return sorted(found)


def local_artifact_ids(poms):
    """Artifact ids produced under integration-tests/, derived rather than hardcoded."""
    local = set()
    for pom in poms:
        try:
            root = ET.parse(pom).getroot()
        except ET.ParseError as e:
            sys.exit(f"{pom}: not well-formed XML: {e}")
        artifact_id = root.find(f"{MAVEN_NS}artifactId")
        if artifact_id is not None and artifact_id.text:
            local.add(artifact_id.text.strip())
    return local


def rewrite(text, local):
    """Return (new_text, number_of_replacements) for one pom."""
    count = 0

    def replace_block(match):
        nonlocal count
        block = match.group(0)
        if OLD not in block:
            return block
        group_id = GROUP_ID.search(block)
        artifact_id = ARTIFACT_ID.search(block)
        if group_id is None or artifact_id is None:
            return block
        if not group_id.group(1).startswith("io.quarkus"):
            return block
        if artifact_id.group(1) in local:
            return block
        count += block.count(OLD)
        return block.replace(OLD, NEW)

    return DEPENDENCY.sub(replace_block, text), count


def main():
    fix = "--fix" in sys.argv[1:]
    if not os.path.isdir(IT_ROOT):
        sys.exit(f"{IT_ROOT}/ not found - run this from the repository root")

    poms = reactor_poms()
    local = local_artifact_ids(poms)

    offenders = []
    total = 0
    for pom in poms:
        with open(pom, encoding="utf-8") as f:
            text = f.read()
        new_text, count = rewrite(text, local)
        if count == 0:
            continue
        offenders.append((pom, count))
        total += count
        if fix:
            with open(pom, "w", encoding="utf-8") as f:
                f.write(new_text)

    if not offenders:
        print(f"OK: {len(poms)} integration test poms, no ${{project.version}} to fix")
        return 0

    for pom, count in offenders:
        print(f"{pom}: {count}")
    if fix:
        print(f"\nRewrote {total} dependencies in {len(offenders)} poms")
        return 0
    print(
        f"\n{total} dependencies in {len(offenders)} poms use ${{project.version}} for a Quarkus\n"
        f"artifact that is not built under {IT_ROOT}/. Use ${{quarkus.version}} instead, so that\n"
        f"the integration tests can also be built against an already released Quarkus.\n"
        f"Run '{sys.argv[0]} --fix' to do this automatically."
    )
    return 1


if __name__ == "__main__":
    sys.exit(main())
