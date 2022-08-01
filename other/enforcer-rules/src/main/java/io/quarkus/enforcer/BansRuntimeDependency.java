package io.quarkus.enforcer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

/**
 * Bans from a deployment module of an extension all runtime dependencies to "foreign" extensions.
 */
public class BansRuntimeDependency extends DeploymentDependencyRuleSupport {

    @Override
    protected void execute(MavenProject project, Map<String, Artifact> nonDeploymentArtifactsByGAV,
            Map<String, Dependency> directDepsByGAV)
            throws EnforcerRuleException {

        String runtimeArtifactId = project.getArtifactId().replace(DEPLOYMENT_ARTIFACT_ID_SUFFIX, "");
        List<String> illegalRuntimeGAVs = nonDeploymentArtifactsByGAV.entrySet().parallelStream()
                .filter(entry -> directDepsByGAV.containsKey(entry.getKey())) // only direct deps
                .filter(entry -> !entry.getValue().getArtifactId().equals(runtimeArtifactId)) // "own" runtime dep is alowed
                .filter(entry -> parseDeploymentGAV(entry.getKey(), entry.getValue()).isPresent())
                .map(entry -> entry.getValue().getArtifactId())
                .sorted()
                .collect(Collectors.toList());

        if (!illegalRuntimeGAVs.isEmpty()) {
            throw new EnforcerRuleException(illegalRuntimeGAVs.size()
                    + " illegal runtime dependencies found that have to be replaced with their "
                    + DEPLOYMENT_ARTIFACT_ID_SUFFIX + " counterparts:\n\t"
                    + illegalRuntimeGAVs.stream().collect(Collectors.joining("\n\t")));
        }
    }

    @Override
    protected boolean isCheckRequired(MavenProject project) {
        return project.getArtifactId().endsWith(DEPLOYMENT_ARTIFACT_ID_SUFFIX);
    }
}
