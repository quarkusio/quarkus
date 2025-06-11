# Quarkus Core Project Analysis

## Project Structure
The io.quarkus:quarkus-core project is part of the Quarkus framework and is structured as follows:

### Main Components
- **Parent POM**: `quarkus-core-parent` - aggregates all core modules
- **Runtime Module**: `quarkus-core` - core runtime components  
- **Other Modules**: deployment, processor, builder, devmode-spi, launcher, class-change-agent, junit4-mock

### Core Runtime Project (io.quarkus:quarkus-core)
- **Location**: `core/runtime/`
- **Type**: Library project
- **Description**: Contains core Quarkus runtime components
- **ArtifactId**: quarkus-core  
- **GroupId**: io.quarkus

## Standard Maven Targets Available

### Lifecycle Phase Targets
- **validate**: Validate project structure
- **compile**: Compile source code (`src/main/java` → `target/classes`)
- **test-compile**: Compile test sources (`src/test/java` → `target/test-classes`)  
- **test**: Run unit tests using Surefire plugin
- **package**: Create JAR artifact (`target/quarkus-core-*.jar`)
- **verify**: Run integration tests and verification
- **install**: Install artifact to local Maven repository
- **deploy**: Deploy artifact to remote repository
- **clean**: Clean build outputs

### Target Dependencies
- test → compile (tests need compiled main code)
- package → test (packaging needs passing tests)
- verify → package (verification needs packaged artifact)
- install → verify (installation needs verified artifact)
- deploy → install (deployment needs installed artifact)

## Key Dependencies
- Jakarta EE APIs (annotations, CDI, injection)
- SmallRye configuration and common utilities
- JBoss logging and thread management
- Quarkus development and bootstrap components
- GraalVM native image support
- JUnit Jupiter for testing

## Project Dependencies Within Quarkus
- quarkus-ide-launcher
- quarkus-development-mode-spi  
- quarkus-bootstrap-runner
- quarkus-extension-processor