# Kotlin Conversion Notes

## NxMavenBatchExecutor Java to Kotlin Conversion

### What Was Done

Converted `NxMavenBatchExecutor.java` to Kotlin with the following changes:

#### Key Kotlin Conversions

1. **Class Structure**: Changed from public class to `object` since all methods are static
2. **Property Declarations**: Used `var` for mutable properties with backing fields
3. **Function Syntax**: Converted Java methods to Kotlin functions with concise syntax
4. **Null Safety**: Applied Kotlin's null safety with nullable types (`String?`)
5. **Collections**: Used Kotlin's mutable collections (`mutableListOf`)
6. **String Templates**: Replaced Java string concatenation with Kotlin string templates
7. **Lambda Expressions**: Simplified lambda syntax for output handlers
8. **Data Classes**: Converted inner classes to `data class` for automatic equals/hashCode/toString

#### Specific Changes

- `System.exit()` → `exitProcess()`
- Java getters/setters → Kotlin properties
- `Arrays.asList()` → `listOf()` or `split()`
- Java lambdas → Kotlin lambdas with simplified syntax
- Java constructors with builders → Kotlin `apply` blocks
- Java ArrayList → Kotlin `mutableListOf()`

#### File Locations

- **Original**: `maven-plugin/src/main/java/NxMavenBatchExecutor.java` (removed)
- **New**: `maven-plugin/src/main/kotlin/NxMavenBatchExecutor.kt`

## Previous Model Classes Converted:
- TargetConfiguration.kt ✓
- TargetMetadata.kt ✓  
- TargetGroup.kt ✓
- TargetDependency.kt ✓
- ProjectMetadata.kt ✓
- RawProjectGraphDependency.kt ✓
- CreateNodesResult.kt ✓
- CreateNodesV2Entry.kt ✓
- ProjectConfiguration.kt ✓

## Status:
NxMavenBatchExecutor has been successfully converted to Kotlin while maintaining the same functionality.