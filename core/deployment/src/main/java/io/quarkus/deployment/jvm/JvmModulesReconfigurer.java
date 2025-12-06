package io.quarkus.deployment.jvm;

import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

/**
 * Interface for reconfiguring JVM module restrictions on the running JVM.
 * It's an interface as I expect us to possibly explore different strategies
 * to accomplish this.
 */
public interface JvmModulesReconfigurer {

    void openJavaModules(List<ModuleOpenBuildItem> addOpens, ModulesClassloaderContext referenceClassloader);

    /**
     * Creates a new instance of {@link JvmModulesReconfigurer}.
     *
     * Initialization of such services is fairly costly: try
     * to avoid it, and aim to reuse the produced instance.
     *
     * @return a new {@link JvmModulesReconfigurer} instance
     */
    static JvmModulesReconfigurer create() {
        final Logger logger = Logger.getLogger("io.quarkus.deployment.jvm");
        JvmModulesReconfigurer reconfigurer = null;
        logger.debugf("Attempting to initialize %s", DirectExportedModulesAPIModulesReconfigurer.class);
        try {
            reconfigurer = new DirectExportedModulesAPIModulesReconfigurer();
        } catch (RuntimeException e) {
            logger.debugf(e, "Failed to initialize %s", DirectExportedModulesAPIModulesReconfigurer.class);
            logger.warn(
                    "Could not get access to jdk.internal.module API: this is required for Quarkus to adjust Java Modules configuration to match the various requirements of each extension. Please ensure this JVM is launched with --add-exports=java.base/jdk.internal.module=ALL-UNNAMED.");
        }
        if (reconfigurer != null) {
            //to avoid nesting try/catch blocks
            return reconfigurer;
        }
        try {
            logger.debugf("Attempting to initialize %s", ReflectiveAccessModulesReconfigurer.class);
            reconfigurer = new ReflectiveAccessModulesReconfigurer();
        } catch (RuntimeException e) {
            //There's no point in warning the user again, that might be confusing. Let's have them try to follow the suggestions related to the above DirectExportedModulesAPIModulesReconfigurer.
            //So, switching to debug logs only:
            logger.debugf(e, "Failed to initialize %s", ReflectiveAccessModulesReconfigurer.class);
        }
        if (reconfigurer != null) {
            //to avoid nesting try/catch blocks
            return reconfigurer;
        }
        try {
            logger.debugf("Attempting to initialize %s", AgentBasedModulesReconfigurer.class);
            reconfigurer = new AgentBasedModulesReconfigurer();
        } catch (RuntimeException e) {
            logger.debugf(e, "Failed to initialize %s", AgentBasedModulesReconfigurer.class);
        }
        if (reconfigurer != null) { //to avoid nesting try/catch blocks
            return reconfigurer;
        }

        return new FallbackModulesReconfigurer();
    }

}
