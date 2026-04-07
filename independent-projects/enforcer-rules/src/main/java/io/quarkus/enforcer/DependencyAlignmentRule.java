package io.quarkus.enforcer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.enforcer.rule.api.EnforcerLevel;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRule2;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;

/**
 * Custom enforcer rule to ensure dependency versions are aligned with a reference artifact.
 * <p>
 * This rule resolves a reference artifact (e.g., Hibernate Platform) and checks that the versions
 * declared in the project's dependencyManagement/dependencies sections match those in the reference artifact.
 */
public class DependencyAlignmentRule implements EnforcerRule2 {

    /**
     * The reference artifact to check against in the format "groupId:artifactId:version".
     * Example: "org.hibernate.orm:hibernate-platform:7.3.0.Final"
     * The version can contain property references like "${hibernate-orm.version}".
     * Required.
     */
    private String referenceArtifact;

    /**
     * List of dependencies to check for alignment with the reference artifact.
     * Each entry specifies the artifact coordinates (groupId:artifactId) to check.
     */
    private List<DependencyAlignmentData> dependencies;

    private Log logger;

    private EnforcerLevel level = EnforcerLevel.ERROR;

    public void setReferenceArtifact(String referenceArtifact) {
        this.referenceArtifact = referenceArtifact;
    }

    public void setDependencies(List<DependencyAlignmentData> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public EnforcerLevel getLevel() {
        return level;
    }

    public void setLevel(EnforcerLevel level) {
        this.level = level;
    }

    @Override
    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        logger = helper.getLog();

        // Validate configuration
        if (referenceArtifact == null || referenceArtifact.trim().isEmpty()) {
            throw new EnforcerRuleException(
                    "referenceArtifact must be configured (e.g., 'org.hibernate.orm:hibernate-platform:7.3.0.Final')");
        }
        if (dependencies == null || dependencies.isEmpty()) {
            logger.warn("No dependencies configured for alignment check");
            return;
        }

        // Parse the reference artifact GAV
        String[] parts = referenceArtifact.split(":");
        if (parts.length != 3) {
            throw new EnforcerRuleException(
                    "referenceArtifact must be in format 'groupId:artifactId:version', got: " + referenceArtifact);
        }
        String referenceGroupId = parts[0];
        String referenceArtifactId = parts[1];
        String referenceVersion = parts[2];

        logger.debug("Checking alignment with " + referenceArtifact);

        MavenProject project;
        RepositorySystem repositorySystem;
        RepositorySystemSession repositorySession;

        try {
            // With the old enforcer API (3.0.0-M3), we need to use evaluate() to get the project and session
            // The newer API (3.2.1+) would allow cleaner dependency injection with @Inject
            project = (MavenProject) helper.evaluate("${project}");
            repositorySystem = helper.getComponent(RepositorySystem.class);
            // RepositorySystemSession is not available via helper methods, must use property evaluation
            repositorySession = (RepositorySystemSession) helper.evaluate("${repositorySystemSession}");
        } catch (ExpressionEvaluationException | ComponentLookupException e) {
            throw new EnforcerRuleException("Failed to get Maven components from context", e);
        }

        // Get remote repositories directly from the project (no need for property evaluation)
        var remoteRepositories = project.getRemoteArtifactRepositories();

        // Get project's dependency versions
        Map<String, String> projectDependencyVersions = getProjectDependencyVersions(project);

        // Resolve the reference artifact
        Map<String, String> referenceDependencyVersions = resolveReferenceArtifact(
                referenceGroupId, referenceArtifactId, referenceVersion,
                repositorySystem, repositorySession, remoteRepositories);

        // Check each configured dependency
        var errors = new StringBuilder();
        for (var dependency : dependencies) {
            var artifact = dependency.getArtifact();
            var expectedVersion = referenceDependencyVersions.get(artifact);

            // If we can't determine the expected version, always fail
            if (expectedVersion == null) {
                errors.append("  - Artifact '%s' not found in reference artifact\n".formatted(artifact));
                continue;
            }

            var actualVersion = projectDependencyVersions.get(artifact);
            // If the dependency is not in the project, only fail if failOnNotFound is true
            if (actualVersion == null) {
                if (dependency.isFailOnNotFound()) {
                    errors.append("  - Artifact '%s' not found in project's dependencyManagement or dependencies\n"
                            .formatted(artifact));
                } else {
                    logger.debug("Artifact '%s' not found in project (skipping)".formatted(artifact));
                }
                continue;
            }

            if (!expectedVersion.equals(actualVersion)) {
                errors.append("""
                          - Version mismatch for '%s':
                            Project declares version '%s'
                            but reference artifact expects '%s'
                        """.formatted(artifact, actualVersion, expectedVersion));
            } else {
                logger.debug("✓ %s version %s is aligned".formatted(artifact, actualVersion));
            }
        }

        if (!errors.isEmpty()) {
            throw new EnforcerRuleException("Dependency version alignment check failed:\n" + errors);
        }
    }

    /**
     * Extracts dependency versions from the project's dependencyManagement and dependencies sections.
     *
     * @param project the Maven project
     * @return a map of "groupId:artifactId" to version
     */
    private Map<String, String> getProjectDependencyVersions(MavenProject project) {
        Map<String, String> versions = new HashMap<>();

        // First, check dependencyManagement section
        if (project.getDependencyManagement() != null
                && project.getDependencyManagement().getDependencies() != null) {
            project.getDependencyManagement().getDependencies().forEach(dep -> {
                String key = dep.getGroupId() + ":" + dep.getArtifactId();
                if (dep.getVersion() != null) {
                    versions.put(key, dep.getVersion());
                }
            });
            logger.debug("Extracted " + versions.size() + " dependencies from project's dependencyManagement");
        }

        // Also check dependencies section for artifacts not in dependencyManagement
        if (project.getDependencies() != null) {
            int dependenciesAdded = 0;
            for (org.apache.maven.model.Dependency dep : project.getDependencies()) {
                String key = dep.getGroupId() + ":" + dep.getArtifactId();
                // Only add if not already present from dependencyManagement
                if (!versions.containsKey(key) && dep.getVersion() != null) {
                    versions.put(key, dep.getVersion());
                    dependenciesAdded++;
                }
            }
            if (dependenciesAdded > 0) {
                logger.debug("Extracted " + dependenciesAdded + " additional dependencies from project's dependencies section");
            }
        }

        logger.debug("Total: " + versions.size() + " dependencies extracted from project");
        return versions;
    }

    /**
     * Resolves the reference artifact descriptor and extracts its managed dependencies and regular dependencies.
     * Uses Maven's Aether API to read the artifact descriptor without manually parsing the POM.
     *
     * @param groupId the reference artifact groupId
     * @param artifactId the reference artifact artifactId
     * @param version the reference artifact version
     * @param repositorySystem the repository system
     * @param repositorySession the repository session
     * @param remoteRepositories the remote repositories
     * @return a map of "groupId:artifactId" to version
     * @throws EnforcerRuleException if resolution fails
     */
    private Map<String, String> resolveReferenceArtifact(
            String groupId,
            String artifactId,
            String version,
            RepositorySystem repositorySystem,
            RepositorySystemSession repositorySession,
            List<ArtifactRepository> remoteRepositories) throws EnforcerRuleException {
        try {

            // Create artifact coordinates
            var artifact = new DefaultArtifact(groupId, artifactId, "pom", version);

            // Convert Maven repositories to Aether repositories
            var aetherRepositories = remoteRepositories.stream()
                    .map(repo -> new RemoteRepository.Builder(repo.getId(), "default", repo.getUrl()).build())
                    .toList();

            // Read the artifact descriptor to get managed dependencies
            var descriptorRequest = new ArtifactDescriptorRequest();
            descriptorRequest.setArtifact(artifact);
            descriptorRequest.setRepositories(aetherRepositories);

            var descriptorResult = repositorySystem.readArtifactDescriptor(repositorySession, descriptorRequest);

            logger.debug("Resolved reference artifact: " + artifact);

            var versions = new HashMap<String, String>();

            // Extract managed dependencies (from dependencyManagement section)
            var managedDeps = descriptorResult.getManagedDependencies();
            if (managedDeps != null) {
                managedDeps.stream()
                        .map(org.eclipse.aether.graph.Dependency::getArtifact)
                        .filter(a -> a.getVersion() != null && !a.getVersion().isEmpty())
                        .forEach(a -> versions.put(a.getGroupId() + ":" + a.getArtifactId(), a.getVersion()));
                logger.debug("Extracted %d managed dependencies".formatted(managedDeps.size()));
            }

            // Extract regular dependencies (from dependencies section) for those not in managedDependencies
            var deps = descriptorResult.getDependencies();
            if (deps != null) {
                var dependenciesAdded = (int) deps.stream()
                        .map(org.eclipse.aether.graph.Dependency::getArtifact)
                        .filter(a -> a.getVersion() != null && !a.getVersion().isEmpty())
                        .filter(a -> !versions.containsKey(a.getGroupId() + ":" + a.getArtifactId()))
                        .peek(a -> versions.put(a.getGroupId() + ":" + a.getArtifactId(), a.getVersion()))
                        .count();
                if (dependenciesAdded > 0) {
                    logger.debug("Extracted %d additional dependencies from dependencies section"
                            .formatted(dependenciesAdded));
                }
            }

            logger.debug("Total: %d dependencies extracted from reference artifact".formatted(versions.size()));
            return versions;

        } catch (ArtifactDescriptorException e) {
            throw new EnforcerRuleException(
                    "Failed to resolve reference artifact " + groupId + ":" + artifactId + ":" + version, e);
        }
    }

    @Override
    public boolean isCacheable() {
        return false;
    }

    @Override
    public boolean isResultValid(EnforcerRule cachedRule) {
        return false;
    }

    @Override
    public String getCacheId() {
        return null;
    }

    @Override
    public String toString() {
        return "DependencyAlignmentRule{" +
                "referenceArtifact='" + referenceArtifact + '\'' +
                ", dependencies=" + dependencies +
                '}';
    }
}
