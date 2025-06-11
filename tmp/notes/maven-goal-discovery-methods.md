# Fast Maven Goal Discovery Methods

## Quick Summary - Top 3 Fastest Methods

1. **`mvn help:describe -Dplugin=PLUGIN_NAME`** - Lists all goals for a specific plugin
2. **`mvn help:effective-pom`** - Shows all configured plugins with their executions
3. **`mvn help:describe -Dcmd=LIFECYCLE_PHASE`** - Shows all goals bound to a lifecycle phase

## 1. Plugin-Specific Goal Discovery

### List All Goals for a Plugin
```bash
# Quarkus plugin goals
mvn help:describe -Dplugin=quarkus -Ddetail=true

# Compiler plugin goals  
mvn help:describe -Dplugin=compiler -Ddetail=true

# Surefire (test) plugin goals
mvn help:describe -Dplugin=surefire -Ddetail=true

# Spring Boot plugin goals
mvn help:describe -Dplugin=spring-boot -Ddetail=true
```

### Quick Goal List (No Details)
```bash
mvn help:describe -Dplugin=quarkus -Dminimal=true
```

## 2. Project-Wide Goal Discovery

### All Configured Plugins and Goals
```bash
# Shows effective POM with all plugin configurations
mvn help:effective-pom

# Filter for plugins only
mvn help:effective-pom | grep -A10 -B2 "<plugin>"
```

### Lifecycle Phase Analysis
```bash
# See what goals run in compile phase
mvn help:describe -Dcmd=compile

# See what goals run in test phase
mvn help:describe -Dcmd=test

# See what goals run in package phase
mvn help:describe -Dcmd=package
```

## 3. Live Goal Discovery

### List All Available Plugins
```bash
# List all plugins in current project
mvn help:evaluate -Dexpression=project.build.plugins -q -DforceStdout
```

### Execute with Dry Run
```bash
# See what would execute without running
mvn compile -DdryRun=true
mvn package -DdryRun=true
```

## 4. IDE and Tool Integration

### IntelliJ IDEA
- Maven tool window → Lifecycle + Plugins sections
- Right-click pom.xml → Maven → Show Effective POM

### VS Code
- Maven for Java extension
- Command Palette → "Java: Build Workspace"

### Command Line Tools
```bash
# Maven wrapper info
./mvnw help:describe -Dplugin=help

# List all available Maven goals in project
./mvnw help:describe -Dcmd=help:describe
```

## 5. Quarkus-Specific Discovery

### Common Quarkus Goals
```bash
# Development mode
mvn quarkus:dev

# Build executable
mvn quarkus:build

# Generate native image  
mvn package -Dnative

# List extensions
mvn quarkus:list-extensions

# Add extension
mvn quarkus:add-extension -Dextensions="extension-name"
```

### Quarkus Plugin Discovery
```bash
mvn help:describe -Dplugin=io.quarkus:quarkus-maven-plugin -Ddetail=true
```

## 6. Advanced Discovery Scripts

### One-Liner to Get All Plugin Goals
```bash
mvn help:effective-pom -q | grep -o 'artifactId>.*maven-plugin</artifactId' | sed 's/artifactId>//g' | sed 's/<.*//g' | while read plugin; do echo "=== $plugin ==="; mvn help:describe -Dplugin=$plugin 2>/dev/null | grep "Goal:"; done
```

### Extract Goals from POM
```bash
# Find all plugin configurations
grep -r "<plugin>" . --include="*.xml" | head -20
```

## 7. Performance Tips

### Speed Up Discovery
1. **Use `-q` flag** to reduce output noise
2. **Use `-o` flag** for offline mode (if dependencies cached)
3. **Use `-T1C` for single-threaded execution** (faster for help goals)
4. **Cache results** in local scripts

### Example Fast Script
```bash
#!/bin/bash
# Fast goal discovery script
echo "=== Common Goals ==="
echo "mvn compile"
echo "mvn test" 
echo "mvn package"
echo "mvn quarkus:dev"
echo "mvn clean install"

echo "=== Available Plugins ==="
mvn help:effective-pom -q | grep "<artifactId>.*maven-plugin</artifactId>" | sort -u
```

## 8. Common Goal Patterns

### Standard Maven Lifecycle
- `validate` → `compile` → `test` → `package` → `verify` → `install` → `deploy`

### Quarkus Development
- `quarkus:dev` - Development mode with hot reload
- `quarkus:test` - Continuous testing
- `quarkus:build` - Build application
- `quarkus:info` - Project information

### Spring Boot
- `spring-boot:run` - Run application
- `spring-boot:build-image` - Build container image
- `spring-boot:repackage` - Create executable jar

## Key Takeaways

1. **`help:describe`** is your primary tool for goal discovery
2. **`help:effective-pom`** shows the complete picture of configured plugins
3. **Plugin prefix shortcuts** like `quarkus:dev` are faster than full coordinates
4. **IDE integration** provides visual goal browsing
5. **Caching Maven help output** speeds up repeated queries