package io.quarkus.deployment.cmd;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;

public class DeployCommandProcessor {
    @BuildStep
    public DeployCommandDeclarationResultBuildItem commandDeclaration(List<DeployCommandDeclarationBuildItem> cmds) {
        if (cmds == null || cmds.isEmpty()) {
            return new DeployCommandDeclarationResultBuildItem(Collections.emptyList());
        }
        return new DeployCommandDeclarationResultBuildItem(
                cmds.stream().map(item -> item.getName()).collect(Collectors.toList()));
    }

    @BuildStep
    public DeployCommandActionResultBuildItem commandExecution(List<DeployCommandActionBuildItem> cmds) {
        if (cmds == null || cmds.isEmpty()) {
            return new DeployCommandActionResultBuildItem(Collections.emptyList());
        }
        return new DeployCommandActionResultBuildItem(cmds);
    }

}
