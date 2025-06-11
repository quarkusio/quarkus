# GroupId:ArtifactId Naming Implementation

## Improvement Made ✅

Updated the Java Maven analyzer to use proper Maven coordinates for project naming.

### Before:
- Projects named like: `bom`, `arc`, `cache`, `jackson`
- Many duplicates due to generic names
- No namespace organization

### After:
- Projects named like: `io.quarkus:quarkus-bom`, `io.quarkus:quarkus-arc`
- Proper Maven coordinates: `groupId:artifactId`
- Clear namespace separation

## Implementation Details:

### Java Code Changes:
```java
String artifactId = model.getArtifactId();
String groupId = model.getGroupId();

// Use parent groupId if not defined locally
if (groupId == null && model.getParent() != null) {
    groupId = model.getParent().getGroupId();
}

// Create unique project name: groupId:artifactId
String projectName = (groupId != null ? groupId : "unknown") + ":" + artifactId;
```

### Benefits:
1. **Unique Naming**: Proper Maven coordinates avoid most naming conflicts
2. **Clear Organization**: Can see which projects belong to which groupId
3. **Standard Convention**: Follows Maven naming standards
4. **Better Dependencies**: Easier to understand project relationships

### Sample Results:
- `io.quarkus:quarkus-core`
- `io.quarkus:quarkus-arc-deployment`  
- `io.quarkus:quarkus-hibernate-orm`
- `org.acme:acme-lib` (from test projects)

The remaining duplicates are primarily test resources with intentionally generic names, which is expected behavior.