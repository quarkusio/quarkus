package io.quarkus.enforcer;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

/**
 * Enforces that for each direct "runtime" dependency the current project also defines a direct minimal "*-deployment"
 * dependency to produce a consistent build order.
 */
public class RequiresMinimalDeploymentDependency implements EnforcerRule2 {

    private static final String GROUP_ID_PREFIX = "io.quarkus";
    private static final String DEPLOYMENT_ARTIFACT_ID_SUFFIX = "-deployment";

    private static final String EXT_PROPERTIES_PATH = "META-INF/quarkus-extension.properties";

    private static final String REQ_TYPE = "pom";
    private static final String REQ_SCOPE = "test";

    private static final String DEP_TEMPLATE = "        <dependency>\n"
            + "            <groupId>%s</groupId>\n"
            + "            <artifactId>%s</artifactId>\n"
            + "            <version>${project.version}</version>\n"
            + "            <type>" + REQ_TYPE + "</type>\n"
            + "            <scope>" + REQ_SCOPE + "</scope>\n"
            + "            <exclusions>\n"
            + "                <exclusion>\n"
            + "                    <groupId>*</groupId>\n"
            + "                    <artifactId>*</artifactId>\n"
            + "                </exclusion>\n"
            + "            </exclusions>\n"
            + "        </dependency>";

    private static final Map<String, Optional<String>> DEPLOYMENT_GAV_CACHE = new ConcurrentHashMap<>();

    private Log logger;

    private EnforcerLevel level = EnforcerLevel.ERROR;

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
        MavenProject project;
        try {
            project = (MavenProject) helper.evaluate("${project}");
        } catch (ExpressionEvaluationException e) {
            throw new IllegalStateException("Failed to get project from EnforcerRuleHelper", e);
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

        String projArtifactKey = buildGAVKey(project.getArtifact());

        Map<String, Dependency> directDepsByGAV = project.getDependencies().stream()
                .filter(d -> d.getGroupId().startsWith(GROUP_ID_PREFIX))
                .collect(Collectors.toMap(d -> d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion(), d -> d,
                        (a, b) -> a));

        List<String> missingDeploymentDeps = nonDeploymentArtifactsByGAV.entrySet().parallelStream()
                .filter(entry -> directDepsByGAV.containsKey(entry.getKey())) // only direct deps
                .map(entry -> DEPLOYMENT_GAV_CACHE.computeIfAbsent(entry.getKey(), k -> parseDeploymentGAV(entry.getValue())))
                .filter(optDeploymentGAV -> optDeploymentGAV
                        .map(deploymentGAV -> !isMinDeploymentDepPresent(deploymentGAV, projArtifactKey, directDepsByGAV))
                        .orElse(false))
                .map(Optional::get)
                .sorted()
                .collect(Collectors.toList());

        if (!missingDeploymentDeps.isEmpty()) {
            String requiredDeps = missingDeploymentDeps.stream()
                    .map(gav -> (Object[]) gav.split(":"))
                    .map(gavArray -> String.format(DEP_TEMPLATE, gavArray))
                    .collect(Collectors.joining("\n"));
            throw new EnforcerRuleException(missingDeploymentDeps.size()
                    + " *-deployment dependencies are missing/configured incorrectly:\n"
                    + "    " + missingDeploymentDeps.stream().collect(Collectors.joining("\n    "))
                    + "\n\nTo fix this issue, add the following dependencies to pom.xml:\n\n"
                    + "        <!-- Minimal test dependencies to *-deployment artifacts for consistent build order -->\n"
                    + requiredDeps);
        }
    }

    private String buildGAVKey(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
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

    private boolean isMinDeploymentDepPresent(String deploymentGAV, String projArtifactKey,
            Map<String, Dependency> directDepsByGAV) {
        return deploymentGAV.equals(projArtifactKey) // special case: current project itself is the "required dependency"
                || Optional.ofNullable(directDepsByGAV.get(deploymentGAV))
                        .filter(d -> REQ_TYPE.equals(d.getType()))
                        .filter(d -> REQ_SCOPE.equals(d.getScope()))
                        .map(d -> d.getExclusions().stream()
                                .anyMatch(e -> "*".equals(e.getGroupId()) && "*".equals(e.getArtifactId())))
                        .orElse(false);
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
}
