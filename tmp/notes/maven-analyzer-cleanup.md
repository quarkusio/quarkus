# Maven Analyzer Cleanup

## What Was Done

Removed unused standalone Maven analyzer code since the codebase has migrated to use the NxAnalyzerMojo Maven plugin instead.

### Files Modified

1. **Deleted**: `maven-plugin-v2/src/main/java/MavenAnalyzer.java`
   - Standalone Java application that parsed pom.xml files directly
   - Was replaced by NxAnalyzerMojo which uses Maven's session APIs

2. **Updated**: `maven-plugin2.ts`
   - Removed unused `generateNxConfigFromMavenAsync` function (lines 178-225)
   - Updated `findJavaAnalyzer` function to look for `NxAnalyzerMojo.class` instead of `MavenAnalyzer.class`

### Architecture Change

**Before**: TypeScript → exec:java MavenAnalyzer → XML parsing → JSON output
**After**: TypeScript → mvn io.quarkus:maven-plugin-v2:analyze → Maven session APIs → JSON output

### Benefits of NxAnalyzerMojo

- Better access to Maven session data and resolved dependencies
- Integration with Maven's project builder
- Automatic workspace discovery via reactorProjects
- More comprehensive analysis capabilities
- No need for external process execution with classpath management

The standalone analyzer approach is no longer needed since the Maven plugin provides superior functionality.