# Fixed nx install with Object Form dependsOn

## Issue
The `nx install io.quarkus:quarkus` command was failing with "Multiple projects matched" error due to how task dependencies were created using simple string format.

## Root Cause
The Maven plugin was generating `dependsOn` as `List<String>` which caused ambiguous dependency resolution when Nx tried to match project patterns.

## Solution Implemented

### 1. Created TargetDependency Model
- Created `model/TargetDependency.java` to represent the object form of Nx dependencies
- Supports `dependencies`, `projects`, `target`, and `params` properties as per Nx documentation

### 2. Updated Core Types
- Changed `TargetConfiguration.dependsOn` from `List<String>` to `List<Object>`
- Updated all related method signatures in:
  - `TargetDependencyService`
  - `NxAnalyzerMojo`
  - `TargetGenerationService`

### 3. Enhanced Dependency Logic
- Cross-module dependencies now use object form: `TargetDependency(target, projects)`
- Same-project dependencies remain as simple strings
- More precise control over which targets depend on which projects

### 4. Fixed Test Compatibility
- Updated test files to handle `List<Object>` instead of `List<String>`
- Added proper type checking in assertions

## Result
✅ `nx install io.quarkus:quarkus-core` now works without "Multiple projects matched" error  
✅ Dependency resolution is more precise with 283 tasks calculated  
✅ Object form allows better specification of cross-project dependencies  

## Files Changed
- `TargetConfiguration.java` - Changed dependsOn type
- `TargetDependency.java` - New model class
- `TargetDependencyService.java` - Updated methods to use object form
- `NxAnalyzerMojo.java` - Updated dependency calculation
- `TargetGenerationService.java` - Updated target generation
- `TargetDependencyServiceTest.java` - Fixed test compatibility

The fix resolves the ambiguity in dependency matching by using the object form of `dependsOn` which allows Nx to precisely understand which targets should depend on which specific projects.