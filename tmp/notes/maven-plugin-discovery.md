# Maven Plugin Discovery Improvements

## Problem
The original code had hardcoded plugin detection logic that was difficult to maintain:
- Required updating code for each new plugin
- Plugin knowledge was scattered throughout the codebase
- No way to discover new plugins dynamically

## Solution
Implemented a dynamic plugin discovery system that uses Maven as the source of truth:

### Two-Tier Approach
1. **Known Plugins**: Fast configuration for common plugins like Quarkus, Spring Boot, Flyway
2. **Unknown Plugins**: Dynamic discovery using `mvn help:describe` command

### Key Components

#### Plugin Configuration Map
- Contains pre-configured goals for well-known plugins
- Includes proper categorization (dev, build, quality, test)
- Optimized for common use cases

#### Dynamic Discovery
- Uses Maven's `help:describe` command to inspect plugins
- Parses Maven output to extract available goals
- Caches results to avoid repeated Maven calls

#### Smart Target Generation
- Generates clean target names (removes maven-plugin suffix)
- Determines appropriate caching behavior
- Sets plugin-specific inputs/outputs
- Categorizes targets into logical groups

### Benefits
- **Maintainable**: No hardcoded plugin lists
- **Extensible**: Works with any Maven plugin
- **Fast**: Caches discovery results
- **Accurate**: Maven is the source of truth
- **Smart**: Optimized configuration for common plugins

### Example
A project with Quarkus plugin automatically gets targets like:
- `dev` - Start Quarkus in development mode
- `quarkus-build` - Build Quarkus application
- `quarkus-generate-code` - Generate sources

Unknown plugins get discovered and their goals become available as targets automatically.