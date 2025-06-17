# Maven Lifecycle APIs for Dynamic Phase Analysis

## Overview
This document details Maven APIs that can replace hardcoded phase behavior analysis in DynamicGoalAnalysisService.analyzeByPhase().

## Current Problem
The current implementation uses a hardcoded switch statement to categorize phases:
- Source processing phases (compile, process-sources, etc.)
- Test phases (test, test-compile, etc.)
- Resource phases (process-resources, etc.)
- Packaging phases (package, verify, install, deploy)

## Maven Lifecycle APIs

### 1. DefaultLifecycles Class
Location: `org.apache.maven.lifecycle.DefaultLifecycles`

Key Methods:
- `getPhaseToLifecycleMap()`: Returns Map<String, Lifecycle> mapping phases to lifecycles
- `getLifeCycles()`: Returns ordered List<Lifecycle> of all lifecycles
- Already injected in ExecutionPlanAnalysisService

### 2. Lifecycle Class
Location: `org.apache.maven.lifecycle.Lifecycle`

Key Methods:
- `getId()`: Returns lifecycle ID ("default", "clean", "site")
- `getPhases()`: Returns List<String> of phases in order
- `getDefaultPhases()`: Returns Map<String, String> (deprecated but useful)

### 3. ExecutionPlanAnalysisService Enhancements
Already has:
- `getLifecycleForPhase(String phase)`: Gets lifecycle containing a phase
- `getAllLifecyclePhases()`: Gets all phases from all lifecycles
- `getDefaultLifecyclePhases()`: Gets default lifecycle phases

## Maven Lifecycle Structure

### Default Lifecycle (23 phases)
1. validate
2. initialize
3. generate-sources
4. process-sources
5. generate-resources
6. process-resources
7. compile
8. process-classes
9. generate-test-sources
10. process-test-sources
11. generate-test-resources
12. process-test-resources
13. test-compile
14. process-test-classes
15. test
16. prepare-package
17. package
18. pre-integration-test
19. integration-test
20. post-integration-test
21. verify
22. install
23. deploy

### Clean Lifecycle (3 phases)
1. pre-clean
2. clean
3. post-clean

### Site Lifecycle (4 phases)
1. pre-site
2. site
3. post-site
4. site-deploy

## Dynamic Phase Categorization Strategy

### Phase Name Analysis
Use semantic analysis of phase names:
- **Source phases**: Contains "source", "compile", or "classes"
- **Test phases**: Contains "test" or known test phases
- **Resource phases**: Contains "resource" 
- **Generation phases**: Contains "generate"
- **Packaging phases**: "package", "verify", "install", "deploy"

### Lifecycle Position Analysis
Use phase position within lifecycle:
- Early phases (validate → compile): Setup/compilation
- Middle phases (test → package): Testing/packaging  
- Late phases (verify → deploy): Verification/deployment

### Implementation Plan
1. Create LifecyclePhaseAnalyzer service
2. Use DefaultLifecycles to get phase metadata
3. Combine phase name patterns with lifecycle position
4. Cache results for performance
5. Provide fallback to current hardcoded logic

## Benefits
- Automatically adapts to new Maven phases
- Works with custom lifecycle phases
- Reduces maintenance of hardcoded phase lists
- More accurate phase behavior detection