# Develocity Build Cache Configuration Analysis

## Root Cause Found

The Quarkus project has **Develocity (Gradle Enterprise) Maven Extension** configured globally through Maven extension files in `.mvn/` directory.

## Key Configuration Files

### 1. `.mvn/extensions.xml`
Loads multiple Develocity extensions:
- `com.gradle:develocity-maven-extension:2.0.1` - Main Develocity extension
- `com.gradle:common-custom-user-data-maven-extension:2.0.3` - Custom data collection
- `com.gradle:quarkus-build-caching-extension:1.9` - **Quarkus-specific build caching**
- `io.quarkus.develocity:quarkus-project-develocity-extension:1.2.2` - Project-specific extension

### 2. `.mvn/develocity.xml`
Configures build cache behavior:

```xml
<buildCache>
    <local>
        <enabled>#{env['RELEASE_GITHUB_TOKEN'] == null and properties['no-build-cache'] == null}</enabled>
    </local>
    <remote>
        <enabled>#{env['RELEASE_GITHUB_TOKEN'] == null and properties['no-build-cache'] == null}</enabled>
        <storeEnabled>#{env['CI'] != null and env['DEVELOCITY_ACCESS_KEY'] != null and env['DEVELOCITY_ACCESS_KEY'] != '' and env['RELEASE_GITHUB_TOKEN'] == null}</storeEnabled>
    </remote>
</buildCache>
```

## Why Our Disable Flags Weren't Working

The cache is controlled by **complex conditional expressions** in `develocity.xml`:
- Cache is enabled when `RELEASE_GITHUB_TOKEN` is null AND `no-build-cache` property is null
- Our environment variables and system properties were not matching the exact conditions

## The Specific Cache Control

The cache is disabled when:
1. `RELEASE_GITHUB_TOKEN` environment variable is set, OR
2. `no-build-cache` **Maven property** is set (not system property)

## Solution Options

### Option 1: Use Maven Property (Recommended)
```bash
mvn clean compile -Dno-build-cache
```

### Option 2: Set Environment Variable
```bash
RELEASE_GITHUB_TOKEN=dummy mvn clean compile
```

### Option 3: Disable Extensions Temporarily
```bash
# Rename extensions file to disable all extensions
mv .mvn/extensions.xml .mvn/extensions.xml.disabled
mvn clean compile
mv .mvn/extensions.xml.disabled .mvn/extensions.xml
```

## Impact on Development

This explains why:
- Code changes weren't being compiled despite `mvn clean compile`
- The "Loaded from the build cache" messages appeared in Maven logs
- Our various disable flags (`gradle.enterprise.build-cache.enabled=false`, etc.) didn't work

## Recommendation

For Maven plugin development work, use:
```bash
mvn clean compile -Dno-build-cache
```

This will ensure fresh compilation of Kotlin/Java source changes.