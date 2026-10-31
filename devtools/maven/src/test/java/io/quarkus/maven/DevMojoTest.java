package io.quarkus.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.junit.jupiter.api.Test;

class DevMojoTest {

    @Test
    void executionsBoundToTheSamePhaseAreOrderedByPriority() {
        // the executions of the process-resources phase as Maven creates them for the quarkus packaging:
        // the priority reflects the order of the goals in the lifecycle mapping, while the plugins come in
        // declaration order, i.e. the quarkus-maven-plugin declared in the POM before the implicit resources plugin
        List<DevMojo.PluginExec> executions = new ArrayList<>();
        executions.add(pluginExec("io.quarkus", "quarkus-maven-plugin", "default-generate-code", "generate-code", -1));
        executions.add(pluginExec("org.acme", "acme-maven-plugin", "acme", "acme", 0));
        executions.add(pluginExec("org.apache.maven.plugins", "maven-resources-plugin", "default-resources", "resources",
                -2));

        DevMojo.sortByPriority(executions);

        assertThat(executions).extracting(DevMojo.PluginExec::getExecutionId)
                .containsExactly("default-resources", "default-generate-code", "acme");
    }

    private static DevMojo.PluginExec pluginExec(String groupId, String artifactId, String executionId, String goal,
            int priority) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        PluginExecution execution = new PluginExecution();
        execution.setId(executionId);
        execution.setPhase("process-resources");
        execution.setGoals(List.of(goal));
        execution.setPriority(priority);
        plugin.addExecution(execution);
        return new DevMojo.PluginExec(plugin, null, execution);
    }
}
