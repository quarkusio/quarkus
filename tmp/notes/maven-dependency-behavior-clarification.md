# Maven Dependency Behavior: I Was Wrong!

You're absolutely right to question this. After researching Maven's actual behavior, I was incorrect about cross-project dependencies.

## Maven DOES Build Dependencies (In Multi-Module Projects)

### Maven Reactor Behavior

Maven has a "reactor" that automatically handles multi-module dependencies:

**When you run `mvn install` in a multi-module project:**
- Maven analyzes ALL modules and their dependencies
- Builds modules in dependency order automatically
- If module A depends on module B, Maven builds B first, then A

### Key Maven Commands

**Single Module (No Dependencies):**
```bash
mvn install -pl :module-name
# Only builds the specified module
```

**Single Module (With Dependencies):**
```bash
mvn install -pl :module-name -am
# Builds the module AND all its dependencies
# -am = --also-make
```

**All Modules:**
```bash
mvn install
# Builds all modules in dependency order
```

## How This Applies to Nx

The current Nx implementation with `^` dependencies is actually CORRECT for mimicking Maven reactor behavior!

```java
// This line IS correct for Maven-like behavior:
dependsOn.add("^" + phase);
```

This makes `nx install core/runtime` equivalent to:
```bash
mvn install -pl :quarkus-core -am
```

## The Real Question

What should `nx install core/runtime` actually do?

**Option 1: Maven Single Module** (no dependencies)
```bash
mvn install -pl :quarkus-core
# Only builds quarkus-core
```

**Option 2: Maven Reactor Style** (with dependencies)  
```bash
mvn install -pl :quarkus-core -am
# Builds all dependencies first, then quarkus-core
```

The current Nx implementation chooses Option 2, which is actually MORE helpful for developers since it ensures a complete build.

## Conclusion

I was wrong - Maven DOES build dependency projects in multi-module scenarios. The current Nx behavior with `^` dependencies correctly mimics Maven reactor builds.

The question isn't whether to remove `^` dependencies, but whether `nx install` should behave like:
- `mvn install -pl :module` (single module only)
- `mvn install -pl :module -am` (module + dependencies) ← Current Nx behavior

The current Nx approach is actually the more useful one for development workflows.