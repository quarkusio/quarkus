package io.quarkus.deployment.jvm;

import java.util.List;

import io.quarkus.deployment.builditem.ModuleExportBuildItem;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

class NoopJvmModulesReconfigurer implements JvmModulesReconfigurer {

    static final NoopJvmModulesReconfigurer INSTANCE = new NoopJvmModulesReconfigurer();

    @Override
    public void openJavaModules(List<ModuleOpenBuildItem> addOpens, ModulesClassloaderContext referenceClassloader) {
        // noop
    }

    @Override
    public void exportJavaModules(List<ModuleExportBuildItem> addExports, ModulesClassloaderContext referenceClassloader) {
        // noop
    }
}
