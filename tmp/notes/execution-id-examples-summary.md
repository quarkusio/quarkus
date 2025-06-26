# Maven Plugin Execution ID Examples from Quarkus Codebase

This document summarizes real-world examples of Maven plugin executions with custom execution IDs found in the Quarkus project. These examples demonstrate where execution ID-based target naming would be more meaningful than goal-based naming in our Nx Maven plugin.

## Examples Found

### 1. Build Parent POM (`/build-parent/pom.xml`)

#### Quarkus Extension Maven Plugin
```xml
<plugin>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-extension-maven-plugin</artifactId>
    <version>${project.version}</version>
    <executions>
        <execution>
            <id>generate-extension-descriptor</id>
            <goals>
                <goal>extension-descriptor</goal>
            </goals>
            <phase>process-resources</phase>
            <configuration>
                <deployment>${project.groupId}:${project.artifactId}-deployment:${project.version}</deployment>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Target Naming Comparison:**
- **Goal-based**: `quarkus-extension:extension-descriptor`
- **Execution ID-based**: `quarkus-extension:generate-extension-descriptor`

The execution ID "generate-extension-descriptor" is more descriptive than just "extension-descriptor" goal.

#### Maven Enforcer Plugin
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <executions>
        <execution>
            <id>enforce</id>
            <phase>${maven-enforcer-plugin.phase}</phase>
            <configuration>
                <rules>
                    <dependencyConvergence/>
                    <externalRules>
                        <location>classpath:enforcer-rules/quarkus-banned-dependencies.xml</location>
                    </externalRules>
                </rules>
            </configuration>
            <goals>
                <goal>enforce</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Target Naming Comparison:**
- **Goal-based**: `enforcer:enforce`
- **Execution ID-based**: `enforcer:enforce`

In this case, both are the same, but the execution ID provides explicit context.

#### Forbidden APIs Plugin
```xml
<plugin>
    <groupId>de.thetaphi</groupId>
    <artifactId>forbiddenapis</artifactId>
    <executions>
        <execution>
            <id>verify-forbidden-apis</id>
            <configuration>
                <failOnUnsupportedJava>false</failOnUnsupportedJava>
                <signaturesFiles>
                    <signaturesFile>${maven.multiModuleProjectDirectory}/.forbiddenapis/banned-signatures-common.txt</signaturesFile>
                </signaturesFiles>
            </configuration>
            <phase>${forbiddenapis-maven-plugin.phase}</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Target Naming Comparison:**
- **Goal-based**: `forbiddenapis:check`
- **Execution ID-based**: `forbiddenapis:verify-forbidden-apis`

The execution ID "verify-forbidden-apis" is much more descriptive than just "check".

#### Revapi Plugin (Multiple Executions)
```xml
<plugin>
    <groupId>org.revapi</groupId>
    <artifactId>revapi-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>api-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <phase>verify</phase>
        </execution>
        <execution>
            <id>api-report</id>
            <goals>
                <goal>report</goal>
            </goals>
            <phase>package</phase>
        </execution>
    </executions>
</plugin>
```

**Target Naming Comparison:**
- **Goal-based**: `revapi:check` and `revapi:report`
- **Execution ID-based**: `revapi:api-check` and `revapi:api-report`

This demonstrates a plugin with multiple executions where execution IDs provide better distinction.

#### Jacoco Plugin
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
            <configuration>
                <includes>
                    <include>io.quarkus*</include>
                </includes>
                <propertyName>jacoco.activated.agent.argLine</propertyName>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Target Naming Comparison:**
- **Goal-based**: `jacoco:prepare-agent`
- **Execution ID-based**: `jacoco:agent`

The execution ID "agent" is shorter and more intuitive than "prepare-agent".

### 2. Tools Parent POM (`/independent-projects/tools/pom.xml`)

#### Import Sort Maven Plugin
```xml
<plugin>
    <groupId>net.revelc.code</groupId>
    <artifactId>impsort-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>sort-imports</id>
            <goals>
                <goal>sort</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Target Naming Comparison:**
- **Goal-based**: `impsort:sort`
- **Execution ID-based**: `impsort:sort-imports`

The execution ID "sort-imports" is more descriptive than the generic "sort" goal.

#### Import Sort Check Execution
```xml
<execution>
    <id>check-imports</id>
    <goals>
        <goal>check</goal>
    </goals>
</execution>
```

**Target Naming Comparison:**
- **Goal-based**: `impsort:check`
- **Execution ID-based**: `impsort:check-imports`

The execution ID "check-imports" is more descriptive than the generic "check" goal.

### 3. Extension Maven Plugin (`/independent-projects/extension-maven-plugin/pom.xml`)

#### Sisu Maven Plugin
```xml
<plugin>
    <groupId>org.eclipse.sisu</groupId>
    <artifactId>sisu-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>index-project</id>
            <goals>
                <goal>main-index</goal>
                <goal>test-index</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Target Naming Comparison:**
- **Goal-based**: `sisu:main-index` and `sisu:test-index`
- **Execution ID-based**: `sisu:index-project`

This shows an execution with multiple goals where the execution ID provides a unified context.

#### Maven Plugin Plugin
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-plugin-plugin</artifactId>
    <executions>
        <execution>
            <id>help-goal</id>
            <goals>
                <goal>helpmojo</goal>
            </goals>
        </execution>
        <execution>
            <id>default-descriptor</id>
            <phase>process-classes</phase>
        </execution>
    </executions>
</plugin>
```

**Target Naming Comparison:**
- **Goal-based**: `plugin:helpmojo`
- **Execution ID-based**: `plugin:help-goal`

The execution ID "help-goal" is more intuitive than "helpmojo".

## Benefits of Execution ID-Based Target Naming

1. **More Descriptive Names**: Execution IDs often provide more context than generic goal names like "check", "sort", "enforce"

2. **Better Distinguishability**: When plugins have multiple executions, execution IDs provide better distinction than goals

3. **Human-Readable Intent**: Execution IDs typically describe what the execution does in business terms

4. **Consistency**: Using execution IDs provides a consistent naming pattern across different plugins

## Implementation Strategy

Based on these examples, our Maven plugin should prioritize execution IDs when:
1. An execution has a custom (non-default) ID
2. The execution ID is more descriptive than the goal name
3. Multiple executions exist for the same plugin

The fallback to goal-based naming should only occur when:
1. No custom execution ID is provided (uses Maven's default)
2. The execution ID is less descriptive than the goal name