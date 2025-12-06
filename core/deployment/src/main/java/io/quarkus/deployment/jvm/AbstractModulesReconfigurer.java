package io.quarkus.deployment.jvm;

import org.jboss.logging.Logger;

import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

/**
 * Convenience base class for {@link JvmModulesReconfigurer} implementations.
 */
abstract class AbstractModulesReconfigurer {

    protected static final Logger logger = Logger.getLogger("io.quarkus.deployment.jvm");

    protected static void warnModuleGetsSkipped(String m, ModuleOpenBuildItem addOpens) {
        logger.warnf("Module %s not found, skipping processing of ModuleOpenBuildItem: %s", m, addOpens);
    }

}
