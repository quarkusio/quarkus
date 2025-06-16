# Maven Plugin Testing Best Practices Research

## Overview
This document compiles research on Maven plugin testing approaches, focusing on official Apache Maven recommendations that minimize mocking and provide real integration testing capabilities.

## Testing Strategy Philosophy

### Unit Testing vs Integration Testing
- **Unit Testing**: Tests mojo as "isolated unit" by mocking Maven environment
  - Fast execution
  - Focuses on individual component verification
  - Uses maven-plugin-testing-harness
- **Integration Testing**: Tests plugin in "real Maven build" context
  - Slower but comprehensive
  - Catches environment-specific issues
  - Uses maven-invoker-plugin or maven-verifier

### Apache Maven Recommendation
"The general wisdom is that your code should be mostly tested with unit tests, but should also have some functional tests."

## Maven Plugin Testing Harness (Unit Testing)

### Purpose and Philosophy
- Pre-constructs Plexus components
- Provides stub objects for Maven functionality (projects, etc.)
- Populates fields from XML configuration resembling plugin POM
- **Minimal Mocking Approach**: Not full mocking, but provides enough Maven context

### Setup Dependencies
```xml
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
```

### Implementation Approaches

#### Option 1: AbstractMojoTestCase (Traditional)
```java
public class MyMojoTest extends AbstractMojoTestCase {
    
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testMojoGoal() throws Exception {
        File pom = getTestFile("src/test/resources/unit/project-to-test/pom.xml");
        MyMojo mojo = (MyMojo) lookupMojo("mygoal", pom);
        assertNotNull(mojo);
        mojo.execute();
        // Assertions
    }
}
```

#### Option 2: MojoRule (JUnit 4.10+, Recommended)
```java
public class MyMojoTest {
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
            // Setup
        }
        @Override
        protected void after() {
            // Cleanup
        }
    };
    
    @Test
    public void testMojoGoal() throws Exception {
        File pom = rule.getTestFile("src/test/resources/unit/project-to-test/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());
        
        MyMojo mojo = (MyMojo) rule.lookupMojo("mygoal", pom);
        assertNotNull(mojo);
        mojo.execute();
        // Assertions
    }
    
    @Test
    @WithoutMojo  // Skip rule execution for this test
    public void testUtilityMethod() {
        // Test that doesn't need Maven context
    }
}
```

### Test Project Structure
```
src/test/resources/unit/project-to-test/
├── pom.xml          # Test project POM with plugin configuration
└── src/            # Optional test project sources
```

### Test Project POM Example
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>test</groupId>
    <artifactId>test-project</artifactId>
    <version>1.0</version>
    
    <build>
        <plugins>
            <plugin>
                <groupId>your.group</groupId>
                <artifactId>your-plugin</artifactId>
                <configuration>
                    <parameter>value</parameter>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## Maven Invoker Plugin (Integration Testing)

### Purpose
- Runs complete Maven builds with your plugin
- Tests plugin in real Maven environment
- Handles artifact staging and repository isolation

### Project Structure
```
./
├── pom.xml
└── src/
    └── it/
        ├── settings.xml         # Optional global settings
        ├── first-integration-test/
        │   ├── pom.xml
        │   ├── invoker.properties  # Test-specific config
        │   └── verify.bsh          # Post-build verification
        └── second-integration-test/
            ├── pom.xml
            └── verify.groovy
```

### Plugin Configuration
```xml
<plugin>
    <artifactId>maven-invoker-plugin</artifactId>
    <version>3.9.0</version>
    <configuration>
        <cloneProjectsTo>${project.build.directory}/it</cloneProjectsTo>
        <settingsFile>src/it/settings.xml</settingsFile>
        <localRepositoryPath>${project.build.directory}/local-repo</localRepositoryPath>
        <postBuildHookScript>verify</postBuildHookScript>
        <goals>
            <goal>clean</goal>
            <goal>compile</goal>
        </goals>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>install</goal>  <!-- Stage artifacts -->
                <goal>run</goal>      <!-- Run tests -->
            </goals>
        </execution>
    </executions>
</plugin>
```

### Invoker Properties Example
```properties
# invoker.properties
invoker.goals = clean compile your-plugin:your-goal
invoker.buildResult = success
invoker.mavenOpts = -Xmx512m
```

### Verification Script Example (BeanShell)
```java
// verify.bsh
import java.io.*;

File expectedFile = new File(basedir, "target/generated-file.txt");
if (!expectedFile.exists()) {
    throw new RuntimeException("Expected file not found: " + expectedFile);
}

String content = FileUtils.fileRead(expectedFile);
if (!content.contains("expected-content")) {
    throw new RuntimeException("File does not contain expected content");
}

return true;
```

## Alternative Testing Frameworks

### Takari Plugin Testing
- Superior alternative to maven-plugin-testing-harness
- Better integration with modern testing practices
- Reduced boilerplate

### Maven Verifier
- Lower-level API for launching Maven
- Good for custom testing scenarios
- More complex setup but more control

## Best Practices Summary

### For Current Maven Plugin Project

1. **Start with Unit Tests** using maven-plugin-testing-harness
   - Use MojoRule approach (JUnit 4+)
   - Create minimal test POMs in src/test/resources
   - Test core mojo functionality

2. **Add Integration Tests** using maven-invoker-plugin
   - Test realistic scenarios
   - Verify plugin works in complete Maven builds
   - Test edge cases and error conditions

3. **Recommended Test Mix**
   - 70-80% unit tests (fast feedback)
   - 20-30% integration tests (comprehensive validation)

### Current Project Status
- Maven plugin exists at `/maven-plugin/`
- No test infrastructure currently in place
- Ready for implementing comprehensive testing strategy

## Implementation Plan

1. Add testing dependencies to maven-plugin/pom.xml
2. Create unit test structure with MojoRule
3. Set up integration test framework with maven-invoker-plugin
4. Create test projects for various scenarios
5. Implement verification scripts for integration tests

## Key Advantages of This Approach

- **Minimal Mocking**: Testing harness provides real Maven context
- **Real Integration**: Invoker plugin tests complete Maven builds  
- **Official Support**: Both are Apache Maven official tools
- **Comprehensive Coverage**: Unit tests for speed, integration tests for completeness
- **Maintainable**: Well-documented, standard approaches