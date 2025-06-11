# Maven Phase Target Design

## Target Configuration Structure

Each Maven phase will be mapped to an NX target with the following structure:

```typescript
{
  executor: '@nx/run-commands:run-commands',
  options: {
    command: 'mvn <phase>',
    cwd: '{projectRoot}'
  },
  dependsOn: ['<previous-phase>'], // Phase dependencies
  inputs: ['{projectRoot}/src/**/*', '{projectRoot}/pom.xml'],
  outputs: ['{projectRoot}/target/**/*']
}
```

## Phase Mapping

### Core Lifecycle Phases
1. **validate** - No dependencies, basic validation
2. **compile** - Depends on validate, outputs to target/classes
3. **test** - Depends on compile, runs unit tests  
4. **package** - Depends on test, creates JAR/WAR
5. **verify** - Depends on package, runs integration tests
6. **install** - Depends on verify, installs to local repo
7. **deploy** - Depends on install, deploys to remote repo

### Additional Phases
8. **clean** - Independent, cleans target directory
9. **site** - Independent, generates documentation

## Implementation Plan

1. Extend `normalizeTargets` function to include Maven phases
2. Add phase detection logic based on POM analysis
3. Create proper dependency chains between phases
4. Add appropriate inputs/outputs for each phase
5. Handle special cases (Spring Boot, Quarkus-specific targets)

## Special Considerations

- Some projects may not support all phases
- Framework-specific plugins may override default behavior
- Need to detect if a project is executable vs library
- Consider performance impact of too many targets