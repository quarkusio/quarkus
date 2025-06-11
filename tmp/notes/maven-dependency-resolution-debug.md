# Maven Dependency Resolution Debug

## Issue
Expected dependency from "io.quarkus.arc.arc" to "io.quarkus.quarkus-core" not appearing in graph.json

## Investigation

### POM File Analysis
- Source: `/home/jason/projects/triage/java/quarkus/extensions/arc/runtime/pom.xml`
- Source project artifactId: `quarkus-arc`
- Source project groupId: `io.quarkus` (inherited from parent)
- But also has dependency on `io.quarkus.arc:arc`

### Expected Dependencies
From POM dependencies:
1. `io.quarkus.arc:arc` → should resolve to `io.quarkus.arc.arc`
2. `io.quarkus:quarkus-core` → should resolve to `io.quarkus.quarkus-core`

### Graph Analysis
- Both projects exist in graph.json:
  - "io.quarkus.arc.arc" at line 79866
  - "io.quarkus.quarkus-core" at line 127215
- But "io.quarkus.arc.arc" has empty dependencies: `"io.quarkus.arc.arc": []`

### Analysis Results

After investigation, the dependency resolution is actually working correctly:

1. **io.quarkus.arc.arc** (from `independent-projects/arc/runtime/pom.xml`):
   - Has empty dependency list: `"io.quarkus.arc.arc": []` 
   - This is correct - only depends on external libraries (jakarta.*, io.smallrye.reactive, etc.)
   - No internal workspace dependencies

2. **io.quarkus.quarkus-arc** (from `extensions/arc/runtime/pom.xml`):
   - Has correct dependencies:
     - `io.quarkus.arc.arc` ✓
     - `io.quarkus.quarkus-core` ✓

### Conclusion
The dependency graph is actually correct. The user expected `io.quarkus.arc.arc` to depend on `io.quarkus.quarkus-core`, but:
- `io.quarkus.arc.arc` is the low-level CDI runtime that only depends on external libraries
- `io.quarkus.quarkus-arc` is the Quarkus integration that depends on both the arc runtime AND quarkus-core

This is the correct architecture - the core arc library is independent of Quarkus.