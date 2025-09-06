package io.quarkus.deployment.jvm;

import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

/**
 * A fallback implementation of {@link JvmModulesReconfigurer} that handles scenarios where
 * we were not allowed to install an agent or get our way via reflection.
 *
 * This implementation will not try to fix things, but reports warnings for all operations which we'd normally
 * have performed.
 */
final class FallbackModulesReconfigurer implements JvmModulesReconfigurer {

    private static final Logger logger = Logger.getLogger("io.quarkus.deployment.jvm");

    @Override
    public void openJavaModules(final List<ModuleOpenBuildItem> addOpens) {
        for (ModuleOpenBuildItem addOpen : addOpens) {
            logger.warnf("Could not automatically install and add-opens for module %s/%s, to module %s",
                    addOpen.openedModule().getName(), addOpen.packageNames(),
                    addOpen.openingModule().isNamed() ? addOpen.openingModule().getName() : "UNNAMED");
        }
    }

}
