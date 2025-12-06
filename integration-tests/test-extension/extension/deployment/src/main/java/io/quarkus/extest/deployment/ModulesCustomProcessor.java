package io.quarkus.extest.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

public class ModulesCustomProcessor {

    @BuildStep
    ModuleOpenBuildItem openModules() {
        return new ModuleOpenBuildItem("java.base", ModuleOpenBuildItem.ALL_UNNAMED, "java.util");
    }
}
