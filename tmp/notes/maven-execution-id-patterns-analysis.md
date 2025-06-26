# Maven Execution ID Patterns Analysis

## Summary
This document analyzes Maven execution ID patterns found in the Quarkus codebase to understand how multiple execution blocks are used for the same Maven goal with different execution IDs.

## Key Findings from Existing Documentation

Based on the analysis of existing notes and codebase examination, several important patterns emerge for Maven plugin executions with custom execution IDs.

### 1. Multiple Executions of Same Goal with Different IDs

#### Revapi Plugin Example (from build-parent/pom.xml)
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

**Target Naming Impact:**
- Goal-based naming would create: `revapi:check` and `revapi:report`
- Execution ID-based naming creates: `revapi:api-check` and `revapi:api-report`

This shows how execution IDs provide better context than generic goal names.

#### Formatter Plugin - Different Execution IDs for Same Goal
```xml
<!-- Format profile -->
<plugin>
    <groupId>net.revelc.code.formatter</groupId>
    <artifactId>formatter-maven-plugin</artifactId>
    <executions>
        <execution>
            <phase>process-sources</phase>
            <goals>
                <goal>format</goal>
            </goals>
        </execution>
    </executions>
</plugin>

<!-- Validate profile -->  
<plugin>
    <groupId>net.revelc.code.formatter</groupId>
    <artifactId>formatter-maven-plugin</artifactId>
    <executions>
        <execution>
            <phase>process-sources</phase>
            <goals>
                <goal>validate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

This demonstrates how the same plugin can have different goals in different profiles, but with different execution contexts.

#### Import Sort Plugin - Multiple Execution IDs for Different Goals
```xml
<!-- Format execution -->
<execution>
    <id>sort-imports</id>
    <goals>
        <goal>sort</goal>
    </goals>
</execution>

<!-- Validation execution -->
<execution>
    <id>check-imports</id>
    <goals>
        <goal>check</goal>
    </goals>
</execution>
```

**Target Naming Comparison:**
- Goal-based: `impsort:sort` and `impsort:check`
- Execution ID-based: `impsort:sort-imports` and `impsort:check-imports`

The execution IDs provide much more descriptive names than the generic `sort` and `check` goals.

### 2. Single Execution with Multiple Goals

#### Sisu Plugin Example
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

**Target Naming Impact:**
- Goal-based would create: `sisu:main-index` and `sisu:test-index` (separate targets)
- Execution ID-based could create: `sisu:index-project` (unified target)

This shows how execution IDs can provide unified context for related goals.

#### Maven Invoker Plugin Example
```xml
<plugin>
    <artifactId>maven-invoker-plugin</artifactId>
    <executions>
        <execution>
            <id>integration-test</id>
            <goals>
                <goal>install</goal>
                <goal>run</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Target Naming Impact:**
- Goal-based: `invoker:install` and `invoker:run`
- Execution ID-based: `invoker:integration-test`

The execution ID "integration-test" provides better context about the purpose than the individual goal names.

### 3. Meaningful vs Default Execution IDs

#### Examples of Meaningful Execution IDs
- `generate-extension-descriptor` (Quarkus Extension Plugin)
- `verify-forbidden-apis` (Forbidden APIs Plugin)
- `sort-imports` / `check-imports` (Import Sort Plugin)
- `api-check` / `api-report` (Revapi Plugin)
- `dokka-jar` (Maven Jar Plugin for Dokka output)
- `help-goal` (Maven Plugin Plugin)

#### Examples of Default/Generic Execution IDs (filtered out)
- `default-compile`
- `default-testCompile`
- `default-cli`
- `default-*` (any execution ID starting with "default-")

### 4. Current Implementation Strategy

The Maven plugin implements smart execution ID handling:

```kotlin
fun getTargetName(artifactId: String?, goal: String, executionId: String?): String {
    val pluginName = normalizePluginName(artifactId)
    
    // Use execution ID if available and not a default one
    if (!executionId.isNullOrEmpty() && 
        !executionId.startsWith("default-") && 
        executionId != "default-cli") {
        return "$pluginName:$executionId"
    }
    
    // Fall back to goal-based naming
    return "$pluginName:$goal"
}
```

This approach:
1. **Prioritizes meaningful execution IDs** over goal names
2. **Filters out generic default execution IDs** to avoid clutter
3. **Falls back to goal-based naming** when execution IDs aren't meaningful

### 5. Benefits of Execution ID-Based Naming

1. **More Descriptive Names**: Execution IDs often describe the business purpose better than technical goal names
2. **Better Distinguishability**: When multiple executions exist, execution IDs provide clearer distinction
3. **Human-Readable Intent**: Execution IDs typically use business terminology rather than technical Maven goal names
4. **Consistency**: Provides consistent naming pattern across different plugins

### 6. Real-World Examples from Generated Targets

From the actual Maven plugin execution analysis:

1. **Properties Plugin**:
   - Target: `properties:set-system-properties`
   - Execution ID: `"default"`
   - Shows how even "default" execution IDs can be more descriptive than goal names

2. **Maven Enforcer Plugin**:
   - Target: `enforcer:enforce`
   - Execution ID: `"enforce"`
   - Example where execution ID matches goal name but provides explicit context

3. **Buildnumber Plugin**:
   - Target: `buildnumber:create`
   - Execution ID: `"get-scm-revision"`
   - Shows how execution ID `get-scm-revision` is much more descriptive than goal `create`

## Conclusion

The analysis confirms that Maven's execution ID system provides valuable semantic information that enhances target naming in Nx. The current implementation correctly:

1. **Leverages execution IDs** when they provide meaningful context
2. **Filters out generic default execution IDs** that don't add value
3. **Falls back gracefully** to goal-based naming when needed
4. **Maintains Maven's execution semantics** while improving target discoverability

This approach allows Nx to create more intuitive and meaningful target names that better reflect the actual purpose of Maven plugin executions, making the developer experience more productive and understandable.

## Files Referenced

- `/Users/jason/projects/triage/java/quarkus3/build-parent/pom.xml` - Lines 1109-1125 (Revapi plugin multiple executions)
- `/Users/jason/projects/triage/java/quarkus3/independent-projects/enforcer-rules/pom.xml` - Lines 178-241 (Import sort plugin examples)
- `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/main/kotlin/ExecutionPlanAnalysisService.kt` - Lines 361-373 (Target naming strategy)
- `/Users/jason/projects/triage/java/quarkus3/tmp/notes/execution-id-examples-summary.md` - Comprehensive examples
- `/Users/jason/projects/triage/java/quarkus3/tmp/notes/execution-id-analysis-results.md` - Technical implementation details