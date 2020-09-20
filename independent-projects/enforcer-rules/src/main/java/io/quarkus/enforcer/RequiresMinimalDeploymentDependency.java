package io.quarkus.enforcer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

/**
 * Enforces that for each direct "runtime" dependency the current project also defines a direct minimal "*-deployment"
 * dependency to produce a consistent build order.
 */
public class RequiresMinimalDeploymentDependency extends DeploymentDependencyRuleSupport {

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

    @Override
    public void execute(MavenProject project, Map<String, Artifact> nonDeploymentArtifactsByGAV,
            Map<String, Dependency> directDepsByGAV)
            throws EnforcerRuleException {

        String projArtifactKey = buildGAVKey(project.getArtifact());

        List<String> missingDeploymentDeps = nonDeploymentArtifactsByGAV.entrySet().parallelStream()
                .filter(entry -> directDepsByGAV.containsKey(entry.getKey())) // only direct deps
                .map(entry -> parseDeploymentGAV(entry.getKey(), entry.getValue()))
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
}
