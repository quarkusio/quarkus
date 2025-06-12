# Maven Target Groups Implementation

## Overview
Added comprehensive target grouping for Maven lifecycle phases and plugin goals using metadata tags. This allows better organization and discovery of related targets in Nx.

## Phase Target Groups

### Pre-Build Phases
- **cleanup**: `clean`
- **pre-build**: `validate`, `initialize`, `generate-sources`, `process-sources`, `generate-resources`, `process-resources`
- **validation**: `validate`
- **initialization**: `initialize`
- **code-generation**: `generate-sources`
- **source-processing**: `process-sources`
- **resource-generation**: `generate-resources`
- **resource-processing**: `process-resources`

### Build Phases  
- **build**: `compile`, `process-classes`, `prepare-package`, `package`
- **compilation**: `compile`
- **post-compilation**: `process-classes`
- **packaging-prep**: `prepare-package`
- **packaging**: `package`
- **artifact**: `package`

### Test Phases
- **test-prep**: `generate-test-sources`, `process-test-sources`, `generate-test-resources`, `process-test-resources`, `test-compile`, `process-test-classes`
- **testing**: `test`, `integration-test`
- **unit-test**: `test`
- **integration-test**: `pre-integration-test`, `integration-test`, `post-integration-test`
- **quality-assurance**: `verify`

### Distribution Phases
- **distribution**: `install`, `deploy`, `site-deploy`
- **local-install**: `install`
- **remote-deploy**: `deploy`

### Documentation Phases
- **documentation**: `site`, `site-deploy`
- **reporting**: `site`, `pre-site`, `site-deploy`

## Plugin Goal Groups

### Framework-Specific
- **quarkus**: Quarkus plugin goals with `framework`, `dev-server`, `hot-reload`, `native-build`
- **spring-boot**: Spring Boot plugin goals with `framework`, `dev-server`, `docker`, `containerization`, `fat-jar`

### Testing Plugins
- **surefire**: Unit testing with `unit-testing`
- **failsafe**: Integration testing with `integration-testing`
- **jacoco**: Code coverage with `code-coverage`, `testing`

### Build Plugins
- **compiler**: Java compilation with `compilation`, `main-sources`, `test-sources`
- **resources**: Resource processing with `resource-processing`
- **jar/war**: Packaging with `packaging`, `jar`, `war`, `web`

### Quality Assurance
- **enforcer**: Validation with `validation`, `quality-assurance`
- **checkstyle**: Code quality with `code-quality`, `style-check`
- **spotbugs/findbugs**: Bug detection with `code-quality`, `bug-detection`

### Target Type Groups
- **serve**: Development servers with `development`, `server`
- **build**: Build artifacts with `compilation`, `build-artifact`
- **test**: Testing targets with `testing`, `verification`
- **deploy**: Deployment targets with `deployment`, `distribution`
- **utility**: Utility tools with `utility`, `tool`

## Implementation Details

### Metadata Structure
```typescript
metadata: {
  type: 'phase' | 'goal',
  phase?: string,
  plugin?: string,
  goal?: string,
  targetType?: string,
  technologies: ['maven'],
  description: string,
  tags: string[] // The grouping tags
}
```

### Usage
- Tags are stored in `target.metadata.tags`
- Can be used for filtering: `nx run-many --target="*" --projects="tag:maven"`
- Enable target discovery: Find all testing targets with `tag:testing`
- Framework-specific targeting: `tag:quarkus` or `tag:spring-boot`

## Benefits
1. **Better Organization**: Logical grouping of related Maven operations
2. **Improved Discovery**: Easy to find targets by purpose (testing, building, etc.)
3. **Framework Awareness**: Special handling for Quarkus, Spring Boot, etc.
4. **Nx Integration**: Works with Nx's built-in target filtering and project graph
5. **Extensible**: Easy to add new plugins and their corresponding tags