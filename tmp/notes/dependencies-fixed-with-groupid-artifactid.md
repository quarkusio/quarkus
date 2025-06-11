# Dependencies Fixed with GroupId:ArtifactId

## Issue Fixed ✅

The dependencies were broken after implementing `groupId:artifactId` naming because the Java analyzer was still using old simple names for internal dependency references.

## Root Cause:
- **Project Names**: Changed to `groupId:artifactId` format ✅
- **Dependencies**: Still using old `artifactId.replace()` format ❌

## Solution Applied:

### Java Code Fix:
```java
// OLD (broken):
String depName = dep.getArtifactId().replace("quarkus-", "").replace("-", "_");

// NEW (working):
String depName = dep.getGroupId() + ":" + dep.getArtifactId();
```

## Results:
- ✅ **1667 projects** with proper `groupId:artifactId` names
- ✅ **5991 dependencies** correctly mapped between projects
- ✅ Consistent naming format across projects and dependencies

### Example Working Dependencies:
- `io.quarkus:quarkus-arc-deployment` → `io.quarkus:quarkus-core`
- `io.quarkus:quarkus-hibernate-orm` → `io.quarkus:quarkus-arc`
- `io.quarkus:quarkus-resteasy` → `io.quarkus:quarkus-core-deployment`

The Maven plugin now generates proper dependencies with consistent `groupId:artifactId` naming throughout!