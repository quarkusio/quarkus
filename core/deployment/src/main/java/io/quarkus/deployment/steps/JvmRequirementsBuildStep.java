package io.quarkus.deployment.steps;

import java.util.List;

import io.quarkus.deployment.ResolvedJVMRequirements;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

public class JvmRequirementsBuildStep {

    @BuildStep
    ResolvedJVMRequirements resolveJVMRequirements(List<ModuleOpenBuildItem> addOpens) {
        return new ResolvedJVMRequirements(addOpens);
    }
}
