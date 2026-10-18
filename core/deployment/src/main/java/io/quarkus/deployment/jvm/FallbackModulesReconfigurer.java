package io.quarkus.deployment.jvm;

import java.util.List;

import io.quarkus.deployment.builditem.ModuleExportBuildItem;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

/**
 * A fallback implementation of {@link JvmModulesReconfigurer} that handles scenarios where
 * we were not allowed to install an agent or get our way via reflection.
 * <p>
 * This implementation will not try to apply any changes to the runtime, but reports warnings
 * for all operations which we should have performed.
 */
final class FallbackModulesReconfigurer implements JvmModulesReconfigurer {

    @Override
    public void openJavaModules(final List<ModuleOpenBuildItem> addOpens, ModulesClassloaderContext ignored) {
        for (ModuleOpenBuildItem addOpen : addOpens) {
            JVMDeploymentLogger.logger.warnf(
                    "FallbackModulesReconfigurer: Could not apply and add-opens for module %s/%s, to module %s",
                    addOpen.openedModuleName(), addOpen.packageNames(),
                    addOpen.openingModuleName());
        }
    }

    @Override
    public void exportJavaModules(final List<ModuleExportBuildItem> addExports, ModulesClassloaderContext ignored) {
        for (ModuleExportBuildItem addExport : addExports) {
            JVMDeploymentLogger.logger.warnf(
                    "FallbackModulesReconfigurer: Could not apply and add-exports for module %s/%s, to module %s",
                    addExport.exportedModuleName(), addExport.packageNames(),
                    addExport.exportingModuleName());
        }
    }

}
