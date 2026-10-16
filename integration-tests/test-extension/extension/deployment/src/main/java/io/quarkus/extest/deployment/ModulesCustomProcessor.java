package io.quarkus.extest.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ModuleEnableNativeAccessBuildItem;
import io.quarkus.deployment.builditem.ModuleExportBuildItem;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

public class ModulesCustomProcessor {

    @BuildStep
    ModuleOpenBuildItem openModules() {
        return new ModuleOpenBuildItem("java.base", "test-module-fake-name", "java.util");
    }

    @BuildStep
    ModuleExportBuildItem exportModules() {
        return new ModuleExportBuildItem("java.base", "test-module-fake-name-export", "sun.security.x509");
    }

    @BuildStep
    ModuleEnableNativeAccessBuildItem allowNativeLibraryLoad() {
        return new ModuleEnableNativeAccessBuildItem("another-fake-module");
    }

}
