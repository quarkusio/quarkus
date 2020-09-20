package io.quarkus.enforcer;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerLevel;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRule2;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

public abstract class DeploymentDependencyRuleSupport implements EnforcerRule2 {

    protected static final String GROUP_ID_PREFIX = "io.quarkus";
    protected static final String DEPLOYMENT_ARTIFACT_ID_SUFFIX = "-deployment";

    private static final String EXT_PROPERTIES_PATH = "META-INF/quarkus-extension.properties";
    private static final Map<String, Optional<String>> DEPLOYMENT_GAV_CACHE = new ConcurrentHashMap<>();

    protected Log logger;

    private EnforcerLevel level = EnforcerLevel.ERROR;

    @Override
    public final EnforcerLevel getLevel() {
        return level;
    }

    public final void setLevel(EnforcerLevel level) {
        this.level = level;
    }

    @Override
    public final void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        logger = helper.getLog();

        MavenProject project;
        try {
            project = (MavenProject) helper.evaluate("${project}");
        } catch (ExpressionEvaluationException e) {
            throw new IllegalStateException("Failed to get project from EnforcerRuleHelper", e);
        }

        if (!isCheckRequired(project)) {
            return;
        }
        // general notes:
        // - "artifacts" are needed to retrieve the actual jar files
        // - "dependencies" are needed to limit the scope to only the direct dependencies of the current module
        //   and to check for the proper scope, type and exclusions
        // - parents are not gathered here since it is expected that the rule is also active for those parents
        //   or those parents are not relevant

        Map<String, Artifact> nonDeploymentArtifactsByGAV = project.getArtifacts().stream()
                .filter(artifact -> "jar".equals(artifact.getType()))
                .filter(artifact -> artifact.getGroupId().startsWith(GROUP_ID_PREFIX))
                .filter(artifact -> !artifact.getArtifactId().endsWith(DEPLOYMENT_ARTIFACT_ID_SUFFIX))
                .collect(Collectors.toMap(this::buildGAVKey, a -> a));

        // Skip if artifacts are not resolved.
        // To avoid this "soft exit", explicit resolving would be necessary but that is pretty elaborate in an enforcer rule.
        // If the build goal is "late" enough, artifacts for the respective scope *will* be resolved automatically.
        if (nonDeploymentArtifactsByGAV.values().stream().anyMatch(artifact -> !artifact.isResolved())) {
            logger.warn("Skipping rule " + RequiresMinimalDeploymentDependency.class.getSimpleName()
                    + ": Artifacts are not resolved, consider using a later build goal like 'package'.");
            return;
        }

        Map<String, Dependency> directDepsByGAV = project.getDependencies().stream()
                .filter(d -> d.getGroupId().startsWith(GROUP_ID_PREFIX))
                .collect(Collectors.toMap(d -> d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion(), d -> d,
                        (a, b) -> a));

        execute(project, nonDeploymentArtifactsByGAV, directDepsByGAV);
    }

    protected abstract void execute(MavenProject project, Map<String, Artifact> nonDeploymentArtifactsByGAV,
            Map<String, Dependency> directDepsByGAV)
            throws EnforcerRuleException;

    protected final String buildGAVKey(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    protected final Optional<String> parseDeploymentGAV(String gav, Artifact artifact) {
        return DEPLOYMENT_GAV_CACHE.computeIfAbsent(gav, k -> parseDeploymentGAV(artifact));
    }

    private Optional<String> parseDeploymentGAV(Artifact artifact) {
        File artifactFile = artifact.getFile();
        if (artifactFile == null || !artifactFile.exists()) {
            throw new IllegalStateException("Artifact file not found for " + artifact);
        }

        Properties extProperties = new Properties();
        try (ZipFile zipFile = new ZipFile(artifactFile)) {
            ZipEntry entry = zipFile.getEntry(EXT_PROPERTIES_PATH);
            if (entry == null) {
                return Optional.empty();
            }
            extProperties.load(new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + EXT_PROPERTIES_PATH + " from " + artifactFile, e);
        }

        String deploymentGAV = extProperties.getProperty("deployment-artifact");
        if (deploymentGAV == null) {
            throw new IllegalStateException(
                    "deployment-artifact artifact not found in " + EXT_PROPERTIES_PATH + " from " + artifactFile);
        }
        return Optional.of(deploymentGAV);
    }

    protected boolean isCheckRequired(MavenProject project) {
        return true;
    }

    @Override
    public final boolean isCacheable() {
        return false;
    }

    @Override
    public final boolean isResultValid(EnforcerRule cachedRule) {
        return false;
    }

    @Override
    public final String getCacheId() {
        return null;
    }
}
