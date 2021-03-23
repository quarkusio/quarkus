package io.quarkus.enforcer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
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

        Set<String> existingUnmatchedDeploymentDeps = directDepsByGAV.entrySet().stream()
                .filter(entry -> entry.getKey().contains(DEPLOYMENT_ARTIFACT_ID_SUFFIX))
                .filter(entry -> REQ_TYPE.equals(entry.getValue().getType()))
                .filter(entry -> REQ_SCOPE.equals(entry.getValue().getScope()))
                .filter(entry -> entry.getValue().getExclusions().stream()
                        .anyMatch(excl -> "*".equals(excl.getGroupId()) && "*".equals(excl.getArtifactId())))
                .map(Entry::getKey)
                .collect(Collectors.toSet());

        List<String> missingDeploymentDeps = nonDeploymentArtifactsByGAV.entrySet().parallelStream()
                .filter(entry -> directDepsByGAV.containsKey(entry.getKey())) // only direct deps
                .map(entry -> parseDeploymentGAV(entry.getKey(), entry.getValue()))
                .sequential()
                .filter(optDeploymentGAV -> optDeploymentGAV
                        .map(deploymentGAV -> !isMinDeploymentDepPresent(deploymentGAV, projArtifactKey,
                                existingUnmatchedDeploymentDeps))
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
                    + " minimal *-deployment dependencies are missing/configured incorrectly:\n"
                    + "    " + missingDeploymentDeps.stream().collect(Collectors.joining("\n    "))
                    + "\n\nTo fix this issue, add the following dependencies to pom.xml:\n\n"
                    + "        <!-- Minimal test dependencies to *-deployment artifacts for consistent build order -->\n"
                    + requiredDeps);
        }
        if (!existingUnmatchedDeploymentDeps.isEmpty()) {
            Set<String> nonSuperfluous = parseNonSuperfluosArtifactIdsFromProperty(project);
            if (!nonSuperfluous.isEmpty()) {
                existingUnmatchedDeploymentDeps
                        .removeIf(gav -> nonSuperfluous.stream().anyMatch(aid -> gav.contains(":" + aid + ":")));
            }
            if (!existingUnmatchedDeploymentDeps.isEmpty()) {
                String superfluousDeps = existingUnmatchedDeploymentDeps.stream()
                        .map(gav -> "    " + gav)
                        .sorted()
                        .collect(Collectors.joining("\n"));
                throw new EnforcerRuleException(existingUnmatchedDeploymentDeps.size()
                        + " minimal *-deployment dependencies are superfluous and must be removed from pom.xml:\n"
                        + superfluousDeps);
            }
        }
    }

    private boolean isMinDeploymentDepPresent(String deploymentGAV, String projArtifactKey,
            Set<String> existingDeploymentDeps) {
        return deploymentGAV.equals(projArtifactKey) // special case: current project itself is the "required dependency"
                || existingDeploymentDeps.remove(deploymentGAV);
    }

    private Set<String> parseNonSuperfluosArtifactIdsFromProperty(MavenProject project) {
        String propValue = project.getProperties().getProperty("enforcer.requiresMinimalDeploymentDependency.nonSuperfluous");
        if (propValue != null) {
            return Arrays.stream(propValue.split(",")).map(String::trim).collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }
}
