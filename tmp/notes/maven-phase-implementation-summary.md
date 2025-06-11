# Maven Phase and Plugin Goal Detection Implementation

## What We've Built

A comprehensive system that uses the Maven Model API to detect relevant phases and plugin goals for each project, then generates appropriate NX targets.

## Java Analyzer Enhancements

### Phase Detection (`detectRelevantPhases`)
- **Packaging-specific phases**: Different phases for jar, war, pom, maven-plugin packaging
- **Plugin execution phases**: Scans `plugin.getExecutions()` to find bound phases
- **Framework-specific phases**: Detects Spring Boot, Quarkus, and other framework phases

### Plugin Goal Detection (`detectPluginGoals`)
- **Execution-based goals**: Extracts goals from `execution.getGoals()`
- **Framework-specific goals**: Hardcoded detection for common plugins:
  - Spring Boot: `run`, `build-image`, `repackage`
  - Quarkus: `dev`, `build`, `generate-code`, `test`
  - Docker: `build`, `push`
  - Code generation: `generate`

### Goal Filtering (`isUsefulPluginGoal`)
- Filters out internal Maven goals (compile, testCompile)
- Includes development goals (run, dev, serve)
- Includes build goals (build, package, repackage)
- Includes test and deployment goals

## TypeScript Plugin Updates

### Dynamic Target Generation (`generateDetectedTargets`)
- Replaces hardcoded phase targets with detected phases
- Creates plugin goal targets with proper Maven commands
- Avoids duplicate targets

### Smart Target Configuration
- **Phase targets**: Use `mvn <phase>` commands
- **Plugin goal targets**: Use `mvn groupId:artifactId:goal` commands
- **Proper dependencies**: Phase dependencies and goal dependencies
- **Appropriate inputs/outputs**: Source files, POM files, target directories

## Example Output

For a Quarkus project, this will generate targets like:
- `clean`, `compile`, `test`, `package`, `verify`, `install`
- `serve` (from `quarkus:dev`)
- `quarkus:build`
- `quarkus:generate-code`

For a Spring Boot project:
- Standard Maven phases
- `serve` (from `spring-boot:run`) 
- `spring-boot:build-image`
- `spring-boot:repackage`

## Benefits

1. **Project-specific targets**: Only relevant phases and goals for each project
2. **Framework-aware**: Automatically detects Spring Boot, Quarkus, etc.
3. **Plugin-driven**: Uses actual POM configuration instead of assumptions
4. **Performance**: Fewer unnecessary targets per project
5. **Accurate dependencies**: Real phase and plugin dependencies