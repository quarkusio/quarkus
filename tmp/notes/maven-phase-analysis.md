# Maven Phase Analysis for NX Target Generation

## Current State
- The current Maven plugin only generates basic targets: `build`, `test`, and `serve`
- These map to simplified Maven operations but don't expose the full lifecycle
- The Java analyzer (`MavenModelReader.java`) focuses on dependency analysis, not phase mapping

## Maven Lifecycle Phases to Add

### Default Lifecycle (most important)
- `validate` - validate the project is correct
- `compile` - compile source code  
- `test` - run unit tests
- `package` - package compiled code (JAR/WAR)
- `verify` - run integration tests and checks
- `install` - install package to local repository
- `deploy` - deploy package to remote repository

### Clean Lifecycle
- `clean` - clean up artifacts from prior builds

### Site Lifecycle
- `site` - generate project documentation

## Target Configuration Strategy
Each phase should become an NX target with:
- Proper `executor` pointing to Maven runner
- Phase-specific `options` and `command`
- Correct `dependsOn` relationships between phases
- Appropriate `inputs` and `outputs` for caching