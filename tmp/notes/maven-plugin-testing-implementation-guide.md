# Maven Plugin Testing Implementation Guide

## Current Project Analysis

### Existing Plugin Structure
- Location: `/maven-plugin/`
- Main Mojo: `NxAnalyzerMojo.java`
- Goals: `nx:analyze` for Nx integration
- No existing test infrastructure

### Plugin Dependencies
- Maven Plugin API (3.8.8)
- Maven Core for MavenSession
- Maven Model
- Gson for JSON output

## Step-by-Step Implementation Plan

### Phase 1: Unit Testing Setup

#### 1. Update maven-plugin/pom.xml
Add testing dependencies:

```xml
<dependencies>
    <!-- Existing dependencies... -->
    
    <!-- Testing Dependencies -->
    <dependency>
        <groupId>org.apache.maven.plugin-testing</groupId>
        <artifactId>maven-plugin-testing-harness</artifactId>
        <version>4.0.0-alpha-2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.apache.maven</groupId>
        <artifactId>maven-compat</artifactId>
        <version>${maven.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

#### 2. Create Test Directory Structure
```
maven-plugin/
├── src/
│   ├── main/java/
│   └── test/
│       ├── java/
│       │   └── NxAnalyzerMojoTest.java
│       └── resources/
│           └── unit/
│               ├── basic-project/
│               │   └── pom.xml
│               ├── multi-module-project/
│               │   ├── pom.xml
│               │   ├── module-a/pom.xml
│               │   └── module-b/pom.xml
│               └── complex-dependencies/
│                   └── pom.xml
```

#### 3. Basic Unit Test Implementation
```java
// src/test/java/NxAnalyzerMojoTest.java
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;

public class NxAnalyzerMojoTest {
    
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
            // Setup if needed
        }
        
        @Override
        protected void after() {
            // Cleanup if needed
        }
    };
    
    @Test
    public void testBasicProjectAnalysis() throws Exception {
        File pom = rule.getTestFile("src/test/resources/unit/basic-project/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());
        
        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupMojo("analyze", pom);
        assertNotNull(mojo);
        
        // Execute the mojo
        mojo.execute();
        
        // Verify output file was created
        File outputFile = new File(pom.getParentFile(), "nx-project-graph.json");
        assertTrue("Output file should be created", outputFile.exists());
        
        // Additional assertions on output content
        // Parse JSON and verify structure
    }
    
    @Test
    public void testMultiModuleProject() throws Exception {
        File pom = rule.getTestFile("src/test/resources/unit/multi-module-project/pom.xml");
        
        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupMojo("analyze", pom);
        mojo.execute();
        
        // Verify multi-module structure is correctly analyzed
        // Check that dependencies between modules are captured
    }
    
    @Test
    @WithoutMojo
    public void testUtilityMethods() {
        // Test utility methods that don't need Maven context
        // For example, JSON serialization logic
    }
}
```

#### 4. Test Project POMs
```xml
<!-- src/test/resources/unit/basic-project/pom.xml -->
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>test</groupId>
    <artifactId>basic-test-project</artifactId>
    <version>1.0.0</version>
    
    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>io.quarkus</groupId>
                <artifactId>maven-plugin</artifactId>
                <version>999-SNAPSHOT</version>
                <configuration>
                    <outputFile>nx-project-graph.json</outputFile>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Phase 2: Integration Testing Setup

#### 1. Add Maven Invoker Plugin
```xml
<!-- Add to maven-plugin/pom.xml build section -->
<plugin>
    <artifactId>maven-invoker-plugin</artifactId>
    <version>3.9.0</version>
    <configuration>
        <cloneProjectsTo>${project.build.directory}/it</cloneProjectsTo>
        <localRepositoryPath>${project.build.directory}/local-repo</localRepositoryPath>
        <settingsFile>src/it/settings.xml</settingsFile>
        <postBuildHookScript>verify</postBuildHookScript>
        <goals>
            <goal>clean</goal>
            <goal>compile</goal>
            <goal>nx:analyze</goal>
        </goals>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>install</goal>
                <goal>run</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### 2. Integration Test Structure
```
maven-plugin/
└── src/
    └── it/
        ├── settings.xml
        ├── simple-project/
        │   ├── pom.xml
        │   ├── invoker.properties
        │   └── verify.bsh
        ├── quarkus-project/
        │   ├── pom.xml
        │   ├── src/main/java/App.java
        │   └── verify.groovy
        └── error-handling/
            ├── pom.xml
            └── verify.bsh
```

#### 3. Integration Test Examples
```xml
<!-- src/it/simple-project/pom.xml -->
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>it-test</groupId>
    <artifactId>simple-project</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-core</artifactId>
            <version>2.16.12.Final</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>@project.groupId@</groupId>
                <artifactId>@project.artifactId@</artifactId>
                <version>@project.version@</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>analyze</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

```properties
# src/it/simple-project/invoker.properties
invoker.goals = clean compile nx:analyze
invoker.buildResult = success
```

```java
// src/it/simple-project/verify.bsh
import java.io.*;
import java.util.*;
import com.google.gson.*;

// Verify output file exists
File outputFile = new File(basedir, "nx-project-graph.json");
if (!outputFile.exists()) {
    throw new RuntimeException("nx-project-graph.json not found");
}

// Parse and validate JSON structure
String content = new String(Files.readAllBytes(outputFile.toPath()));
JsonObject graph = JsonParser.parseString(content).getAsJsonObject();

// Validate required fields
if (!graph.has("projects")) {
    throw new RuntimeException("Missing 'projects' field in output");
}

if (!graph.has("dependencies")) {
    throw new RuntimeException("Missing 'dependencies' field in output");
}

// Validate project exists
JsonObject projects = graph.getAsJsonObject("projects");
if (!projects.has("simple-project")) {
    throw new RuntimeException("Project 'simple-project' not found in graph");
}

System.out.println("Integration test passed successfully!");
return true;
```

### Phase 3: Advanced Testing Scenarios

#### 1. Error Handling Tests
- Invalid POM structures
- Missing dependencies
- Network failures (if applicable)
- Permissions issues

#### 2. Performance Tests
- Large multi-module projects
- Deep dependency hierarchies
- Memory usage validation

#### 3. Real-world Scenarios
- Quarkus projects with extensions
- Spring Boot projects
- Complex Maven configurations

## Testing Execution Commands

### Run Unit Tests Only
```bash
cd maven-plugin
mvn test
```

### Run Integration Tests Only
```bash
cd maven-plugin
mvn invoker:run
```

### Run All Tests
```bash
cd maven-plugin
mvn clean verify
```

### Run Specific Integration Test
```bash
cd maven-plugin
mvn invoker:run -Dinvoker.test=simple-project
```

## Debugging and Development

### Debug Unit Tests
```bash
mvn -Dmaven.surefire.debug test
```

### Debug Integration Tests
```bash
mvn invoker:run -Dinvoker.debug=true
```

### Inspect Integration Test Output
Integration test projects are copied to `target/it/` for inspection after running.

## Validation Checklist

### Unit Tests Should Cover:
- [ ] Basic mojo execution
- [ ] Parameter validation
- [ ] JSON output format
- [ ] Error handling
- [ ] Multi-module project handling
- [ ] Dependency resolution

### Integration Tests Should Cover:
- [ ] End-to-end plugin execution
- [ ] Real Maven project scenarios
- [ ] Plugin goal integration
- [ ] Output file generation
- [ ] Nx compatibility
- [ ] Error scenarios

## Benefits of This Approach

1. **Comprehensive Coverage**: Both unit and integration testing
2. **Real Maven Context**: Tests run in actual Maven environment
3. **Minimal Mocking**: Uses official Maven testing tools
4. **Maintainable**: Standard Maven testing patterns
5. **CI/CD Ready**: Integrates with standard Maven build lifecycle
6. **Debugging Friendly**: Clear separation of test types and easy debugging options