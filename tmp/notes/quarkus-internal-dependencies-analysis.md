# Quarkus Internal Dependencies Analysis

## Overview
Analysis of core/runtime dependencies to identify which are internal Quarkus modules vs external dependencies.

## Internal Quarkus Dependencies from core/runtime/pom.xml

The following dependencies have groupId "io.quarkus" and are internal to this workspace:

### 1. quarkus-ide-launcher
- **Location**: `/home/jason/projects/triage/java/quarkus/core/launcher/`
- **Status**: ✅ EXISTS as workspace module
- **Module Name**: quarkus-ide-launcher (defined in core/launcher/pom.xml)
- **Purpose**: IDE integration and launcher functionality

### 2. quarkus-development-mode-spi
- **Location**: `/home/jason/projects/triage/java/quarkus/core/devmode-spi/`
- **Status**: ✅ EXISTS as workspace module
- **Module Name**: quarkus-development-mode-spi (defined in core/devmode-spi/pom.xml)
- **Purpose**: SPI for development mode functionality

### 3. quarkus-bootstrap-runner
- **Location**: `/home/jason/projects/triage/java/quarkus/independent-projects/bootstrap/runner/`
- **Status**: ✅ EXISTS as workspace module
- **Module Name**: quarkus-bootstrap-runner (defined in independent-projects/bootstrap/runner/pom.xml)
- **Purpose**: Bootstrap runner for application startup

### 4. quarkus-fs-util
- **Location**: Appears to be part of bootstrap modules but not found as standalone directory
- **Status**: ❌ NOT FOUND as standalone module in workspace
- **Dependencies**: Referenced in bootstrap/maven-resolver and bootstrap/app-model pom.xml files
- **Note**: This might be a utility module that's part of bootstrap-core or another bootstrap module

### 5. quarkus-extension-processor
- **Location**: Not found as standalone module
- **Status**: ❌ NOT FOUND as standalone module in workspace
- **Usage**: Used extensively throughout extensions as annotation processor
- **Note**: This is likely built as part of the core processor module or independent-projects

## Additional Related Modules Found

### quarkus-bootstrap-core
- **Location**: `/home/jason/projects/triage/java/quarkus/independent-projects/bootstrap/core/`
- **Status**: ✅ EXISTS as workspace module
- **Referenced**: In parentFirstArtifacts configuration in core/runtime/pom.xml

## Summary

Out of the 5 internal Quarkus dependencies in core/runtime:

- **3 modules exist** as standalone directories in the workspace:
  1. quarkus-ide-launcher (core/launcher)
  2. quarkus-development-mode-spi (core/devmode-spi)  
  3. quarkus-bootstrap-runner (independent-projects/bootstrap/runner)

- **2 modules not found** as standalone directories:
  1. quarkus-fs-util (might be part of bootstrap-core)
  2. quarkus-extension-processor (likely part of core/processor)

The modules are distributed across:
- `core/` directory: IDE launcher and dev mode SPI
- `independent-projects/bootstrap/` directory: Bootstrap runner and related utilities