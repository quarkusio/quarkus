# Maven Script Usage Analysis

## Current System Architecture

The TypeScript plugin system currently uses the Maven script through the following components:

### 1. Nx Configuration
- **File**: `/home/jason/projects/triage/java/quarkus/nx.json`
- **Plugin**: `./maven-plugin2.ts` is registered as the active Maven plugin

### 2. Maven Plugin Files Found
- `maven-plugin2.ts` - Main plugin using `MavenEmbedderReader`
- `maven-plugin.ts` - Alternative plugin using direct POM parsing
- `maven-plugin-simple.ts` - Simplified plugin using `MavenModelReader`

### 3. Current Maven Script Usage

#### In maven-plugin2.ts (ACTIVE PLUGIN):
- **Line 173**: Uses `MavenEmbedderReader` via Maven exec
- **Line 300**: Batch processing with `MavenEmbedderReader`
- **Command**: `mvn exec:java -Dexec.mainClass="MavenEmbedderReader" -Dexec.args="--hierarchical --nx"`
- **Directory**: Uses `/maven-script/` directory
- **Output**: Writes to `maven-script/maven-results.json`

#### In maven-plugin-simple.ts:
- **Line 114**: Uses `MavenModelReader` via Maven exec
- **Command**: `mvn exec:java -Dexec.mainClass=MavenModelReader -Dexec.args=--stdin --nx`

### 4. Java Classes Used
- `MavenEmbedderReader.java` - Used by maven-plugin2.ts (active)
- `MavenModelReader.java` - Used by maven-plugin-simple.ts (inactive)

### 5. Maven-Plugin-V2 Status
- **Directory**: `/maven-plugin-v2/` exists with `MavenAnalyzer.java`
- **Usage**: Not currently used by any TypeScript plugins
- **Goal**: Replace maven-script usage with maven-plugin-v2

## Required Changes

To update the system to use maven-plugin-v2 instead of maven-script:

1. **Update maven-plugin2.ts** (the active plugin):
   - Change maven script directory from `maven-script` to `maven-plugin-v2`
   - Update main class from `MavenEmbedderReader` to `MavenAnalyzer`
   - Update command line arguments if needed

2. **Test the changes** to ensure maven-plugin-v2 provides compatible output

3. **Optional**: Update maven-plugin-simple.ts if needed for consistency