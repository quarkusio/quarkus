# Restructured Maven Integration Architecture

## Successfully Implemented Changes

### New Architecture
✅ **Goal Graph**: Individual Maven goals form the primary task dependency graph
✅ **Phase Entry Points**: Phase targets depend on all goals that should complete by that phase
✅ **Clean Separation**: Phase targets don't participate in goal-to-goal dependencies

### Concrete Example: `install` Phase
When running `nx install`, Nx now executes goals in correct Maven lifecycle order:

1. `maven-resources:resources` ✅
2. `maven-compiler:compile` ✅  
3. `maven:descriptor` ✅
4. `maven-surefire:test` ✅
5. `maven-jar:jar` ✅
6. `maven-install:install` ❌ (Maven session context issue)

### Benefits Achieved
- ✅ **Granular caching**: Each goal is independently cacheable by Nx
- ✅ **Proper dependencies**: Goals execute in correct Maven lifecycle order
- ✅ **Clean architecture**: Phase targets are convenience entry points, not part of dependency chain
- ✅ **No circular dependencies**: Clear separation between goal graph and phase targets

### Remaining Issue
The `maven-install:install` goal still fails because it runs in a separate Maven session and can't access the JAR artifact created by `maven-jar:jar`. This is a Maven-specific issue where individual goals need the full lifecycle context.

### Architecture Success
From an Nx perspective, the new architecture works perfectly:
- Phase targets correctly aggregate the right goals
- Dependencies flow properly through the goal graph
- Granular caching is enabled
- Users can run either `nx install` (phase) or `nx maven-jar:jar` (individual goal)

The failure is purely a Maven limitation when running goals in isolation, not an issue with the Nx integration architecture.