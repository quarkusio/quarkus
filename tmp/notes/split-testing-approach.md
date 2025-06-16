# Split Testing Approach for io.quarkus:quarkus-core

## Overview

Successfully split the comprehensive quarkus-core tests into multiple focused test files that check specific differences between phases, goals, and other Maven plugin aspects. This approach provides much better granularity and maintainability.

## Created Test Files

### 1. `maven-phases.test.ts` - Maven Lifecycle Phases
**Focus**: Tests specifically for Maven lifecycle phases (validate, compile, test, package, etc.)

**Key Test Areas**:
- Standard Maven lifecycle phase identification
- Phase metadata structure validation
- Phase dependency patterns using `^` syntax
- Cross-project phase consistency
- Phase vs goal distinction

**Sample Tests**:
```typescript
it('should have nx:noop executor for all lifecycle phases')
it('should have proper phase dependencies using ^ syntax') 
it('should distinguish between default lifecycle and clean lifecycle phases')
it('should have empty inputs and outputs for phases')
```

### 2. `maven-goals.test.ts` - Maven Plugin Goals  
**Focus**: Tests specifically for Maven plugin goals (maven-compiler:compile, maven-surefire:test, etc.)

**Key Test Areas**:
- Standard Maven plugin goal identification
- Quarkus-specific plugin goals
- Third-party plugin goals
- Goal metadata and execution phases
- Goal commands and executor configuration

**Sample Tests**:
```typescript
it('should identify maven-compiler plugin goals')
it('should identify quarkus-extension plugin goals') 
it('should have nx:run-commands executor for all goals')
it('should bind goals to correct lifecycle phases')
```

### 3. `cross-module-dependencies.test.ts` - Cross-Module Dependency Patterns
**Focus**: Tests specifically for `^` syntax and cross-module dependency relationships

**Key Test Areas**:
- Target dependency `^` syntax validation
- Project-level vs target-level dependencies  
- Cross-module execution ordering
- Dependency graph structure and circular dependency detection
- Performance impact analysis

**Sample Tests**:
```typescript
it('should identify targets using ^ syntax for cross-module dependencies')
it('should validate ^ syntax patterns for lifecycle phases')
it('should not use ^ syntax for same-module dependencies')
it('should ensure proper execution order across modules')
```

### 4. `target-inputs-outputs.test.ts` - Inputs/Outputs Configuration
**Focus**: Tests specifically for target input/output patterns and caching optimization

**Key Test Areas**:
- Input configuration for different target types
- Output configuration for build/test/deployment targets
- Input/output pattern validation
- Performance optimization analysis
- Cross-project pattern comparison

**Sample Tests**:
```typescript
it('should have pom.xml as input for all Maven targets')
it('should have source inputs for compilation targets')
it('should have target directory outputs for build targets') 
it('should identify targets optimized for incremental builds')
```

## Benefits of Split Approach

### 1. **Focused Testing**
- Each file tests a specific aspect (phases vs goals vs dependencies vs I/O)
- Easier to understand what each test validates
- Clear separation of concerns

### 2. **Better Maintainability**  
- Smaller, more manageable test files
- Easier to add new tests for specific areas
- Clearer test failure debugging

### 3. **Specialized Validation**
- **Phases**: Focus on orchestration and lifecycle management
- **Goals**: Focus on actual Maven plugin execution
- **Dependencies**: Focus on cross-module coordination 
- **I/O**: Focus on caching and performance optimization

### 4. **Comprehensive Coverage**
- Phases: Executor type, metadata, dependency patterns
- Goals: Plugin identification, command generation, phase binding
- Dependencies: `^` syntax usage, graph analysis, ordering
- I/O: Caching patterns, incremental builds, consistency

## Key Differences Tested

### Phases vs Goals
- **Phases**: Use `nx:noop` executor, empty inputs/outputs, orchestration role
- **Goals**: Use `nx:run-commands` executor, specific inputs/outputs, actual work

### Cross-Module vs Local Dependencies  
- **Cross-Module**: Use `^` syntax (e.g., `^compile`, `^test`)
- **Local**: Direct target names or goal references within same project

### Input/Output Patterns
- **Compilation**: Source files + pom.xml → target directory
- **Testing**: Test sources + pom.xml → test reports + target
- **Phases**: Empty inputs/outputs (orchestrators)
- **Deployment**: Inputs only, no local outputs

## Test Execution

Each test file can be run independently:
```bash
npx vitest run maven-phases.test.ts
npx vitest run maven-goals.test.ts  
npx vitest run cross-module-dependencies.test.ts
npx vitest run target-inputs-outputs.test.ts
```

Or all together:
```bash
npx vitest run *phases*.test.ts *goals*.test.ts *dependencies*.test.ts *inputs*.test.ts
```

## Integration with Existing Tests

These focused tests complement the existing `maven-plugin.test.ts` which provides:
- Overall Maven analysis workflow
- Cached result testing
- Integration testing
- Performance benchmarking

The split approach provides deep, specialized testing while the original provides broad integration testing.

## Future Extensions

Each test file can be independently extended:
- **Phases**: Add lifecycle customization tests
- **Goals**: Add plugin configuration validation
- **Dependencies**: Add parallel execution optimization
- **I/O**: Add advanced caching strategies

This modular approach makes the test suite much more maintainable and comprehensive.