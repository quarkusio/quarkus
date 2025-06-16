# Maven Plugin Testing Instructions

This guide provides comprehensive testing instructions for the Nx Maven plugin integration, focusing on `nx graph --graph.json` and `nx show projects` commands.

## Prerequisites

- Nx CLI installed (local: v21.1.3)
- Maven installed (3.9.9+)
- Java Development Kit
- Plugin compiled: `/maven-plugin/target/classes/`

## Quick Start Testing

### 1. Basic Plugin Verification
```bash
# Verify plugin is loaded and working
nx show projects

# Generate project graph JSON
nx graph --file graph.json

# View project details
nx show projects --verbose
```

### 2. Cache Busting Commands

**IMPORTANT**: Always bust caches when testing Java changes to ensure fresh results.

```bash
# Clear all Nx caches
nx reset

# Clear specific cache types
rm -rf .nx/cache
rm -rf node_modules/.cache/nx

# Force fresh plugin execution
NX_CACHE_DIRECTORY="/tmp/nx-test-cache" nx graph --file graph.json

# Complete clean slate (nuclear option)
nx reset && rm -rf .nx && rm -rf node_modules/.cache
```

## Detailed Testing Procedures

### Testing `nx graph --file graph.json`

This command generates a complete project graph with all discovered projects and their dependencies.

#### Expected Output Structure
```json
{
  "version": "6.0",
  "projects": {
    "project-name": {
      "name": "project-name",
      "type": "lib|app",
      "data": {
        "root": "path/to/project",
        "sourceRoot": "path/to/project/src/main/java",
        "targets": {
          "compile": { ... },
          "test": { ... },
          "package": { ... }
        }
      }
    }
  },
  "dependencies": {
    "project-name": [
      {
        "source": "project-name",
        "target": "dependency-project",
        "type": "static"
      }
    ]
  }
}
```

#### Testing Steps

1. **Generate Graph JSON**
   ```bash
   nx graph --graph.json
   ```

2. **Verify File Creation**
   ```bash
   ls -la graph.json
   cat graph.json | jq '.projects | keys | length'  # Count projects
   ```

3. **Validate Project Discovery**
   ```bash
   # Check specific project exists
   cat graph.json | jq '.projects["independent-projects-arc-processor"]'
   
   # List all discovered projects
   cat graph.json | jq '.projects | keys[]' | sort
   
   # Count projects by type
   cat graph.json | jq '.projects | to_entries | group_by(.value.type) | map({type: .[0].value.type, count: length})'
   ```

4. **Verify Target Generation**
   ```bash
   # Check Maven phase targets exist
   cat graph.json | jq '.projects["your-project"].data.targets | keys[]'
   
   # Verify target structure
   cat graph.json | jq '.projects["your-project"].data.targets.compile'
   
   # Check for cross-project dependencies using ^ syntax
   cat graph.json | jq '.projects[].data.targets[].dependsOn[]?' | grep "\^"
   ```

5. **Test Dependency Resolution**
   ```bash
   # View all project dependencies
   cat graph.json | jq '.dependencies'
   
   # Check specific project dependencies
   cat graph.json | jq '.dependencies["your-project"][]'
   ```

### Testing `nx show projects`

This command lists all discovered projects with optional details.

#### Basic Project Listing
```bash
# List all projects
nx show projects

# Show projects with details
nx show projects --verbose

# Filter by type (if supported)
nx show projects --type=lib
nx show projects --type=app
```

#### Detailed Project Analysis
```bash
# Show specific project configuration
nx show project your-project-name

# View project targets
nx show project your-project-name --web
```

### Advanced Testing Scenarios

#### 1. Large Codebase Testing
```bash
# Test performance with large project count
time nx graph --graph.json

# Monitor memory usage
/usr/bin/time -l nx graph --graph.json
```

#### 2. Cross-Module Dependencies
```bash
# Test projects with reactor dependencies
cd independent-projects/arc
nx graph --graph.json
cat graph.json | jq '.dependencies' | grep "\^"
```

#### 3. Plugin Goal Testing
```bash
# Verify framework-specific targets are created
cat graph.json | jq '.projects[].data.targets | to_entries[] | select(.value.metadata.type == "goal") | .key'

# Check for Quarkus dev targets
cat graph.json | jq '.projects[].data.targets["quarkus-dev"]?'

# Verify Spring Boot targets
cat graph.json | jq '.projects[].data.targets["spring-boot-run"]?'
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Plugin Not Loading
```bash
# Check plugin configuration
cat nx.json | jq '.plugins'

# Verify plugin file exists
ls -la maven-plugin.ts

# Check compilation
ls -la maven-plugin/target/classes/
```

#### 2. No Projects Discovered
```bash
# Verify pom.xml files exist
find . -name "pom.xml" | head -10

# Check pattern matching
grep -r "pom.xml" nx.json

# Test with single project
cd path/to/single/project
nx graph --graph.json
```

#### 3. Missing Targets
```bash
# Check Maven plugin compilation
cd maven-plugin && mvn compile

# Verify Java class files
ls maven-plugin/target/classes/io/nx/maven/

# Test with debug output
DEBUG=* nx graph --graph.json
```

#### 4. Dependency Issues
```bash
# Check Maven reactor build
mvn dependency:tree | head -20

# Verify cross-module references
grep -r "^" graph.json
```

### Performance Testing

#### Benchmarking Commands
```bash
# Time graph generation
time nx graph --graph.json

# Memory usage profiling
node --max-old-space-size=8192 $(which nx) graph --graph.json

# Cache hit/miss analysis
rm -rf .nx/cache && time nx graph --graph.json  # Cold
time nx graph --graph.json  # Warm cache
```

#### Expected Performance Metrics
- **Quarkus (400+ projects)**: 30-60 seconds cold, 5-10 seconds warm
- **Medium project (50-100)**: 5-15 seconds cold, 1-3 seconds warm
- **Small project (<20)**: 1-5 seconds cold, <1 second warm

### Debugging Commands

#### Enable Debug Output
```bash
# TypeScript plugin debug
DEBUG=nx:maven-plugin nx graph --graph.json

# Java Maven plugin debug
MAVEN_OPTS="-Ddebug=true" nx graph --graph.json

# Verbose Nx output
nx graph --graph.json --verbose
```

#### Manual Testing
```bash
# Test Maven plugin directly
cd path/to/project
mvn io.nx.maven:nx-maven-plugin:analyze

# Verify output files
ls target/nx-*
cat target/nx-analysis.json
```

## Test Validation Checklist

### Basic Functionality ✓
- [ ] `nx show projects` lists all Maven projects
- [ ] `nx graph --graph.json` generates valid JSON
- [ ] Projects have correct names and paths
- [ ] Basic targets (compile, test, package) exist

### Target Generation ✓
- [ ] Maven lifecycle phases present
- [ ] Plugin goals discovered (quarkus:dev, etc.)
- [ ] Target dependencies follow ^ syntax
- [ ] Inputs/outputs configured correctly

### Dependency Resolution ✓
- [ ] Project dependencies accurate
- [ ] Cross-module dependencies using ^
- [ ] No circular dependencies
- [ ] Parent-child relationships correct

### Performance ✓
- [ ] Reasonable execution time
- [ ] Caching works effectively
- [ ] Memory usage acceptable
- [ ] Large codebases handled

### Integration ✓
- [ ] Works with existing Nx workspace
- [ ] Compatible with other plugins
- [ ] No conflicts with nx.json
- [ ] Clean error messages

## Helper Scripts

Use the existing utility scripts for additional testing:

```bash
# First, generate Maven analysis data
cd maven-plugin
mvn io.quarkus:maven-plugin:999-SNAPSHOT:analyze
cd ..

# Copy analysis data to expected location
cp maven-plugin/maven-analysis.json maven-results.json

# Visualize target structure
node show-nx-targets.js

# Understand dependency chains
node explain-targets.js

# View raw analysis data
cat maven-results.json | jq '.'
```

## Reporting Issues

When reporting issues, include:

1. **Environment**: OS, Node.js, Nx, Maven versions
2. **Commands**: Exact commands that failed
3. **Output**: Full error messages and logs
4. **Project**: Sample project structure or pom.xml
5. **Cache**: Whether issue persists after `nx reset`

Example bug report:
```bash
# Environment
uname -a
node --version
nx --version
mvn --version

# Reproduction
nx reset
DEBUG=* nx graph --graph.json > debug.log 2>&1

# Attach debug.log and relevant pom.xml files
```
