package io.quarkus.deployment.jvm;

import java.lang.instrument.Instrumentation;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.changeagent.ClassChangeAgent;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;
import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * Interface for reconfiguring JVM module restrictions on the running JVM.
 * It's an interface as I expect us to possibly explore different strategies
 * to accomplish this.
 */
public interface JvmModulesReconfigurer {

    void openJavaModules(List<ModuleOpenBuildItem> addOpens, ModulesClassloaderContext referenceClassloader);

    /**
     * Thread-safe lazy holder for the singleton instance: this is expensive to create and tied to the JVM,
     * so we like it to be lazy and shared.
     */
    final class Holder {
        static final JvmModulesReconfigurer INSTANCE = JvmModulesReconfigurer.create();
    }

    /**
     * @return the shared {@link JvmModulesReconfigurer} instance
     */
    static JvmModulesReconfigurer getInstance() {
        return Holder.INSTANCE;
    }

    private static JvmModulesReconfigurer create() {
        final Logger logger = JVMDeploymentLogger.logger;

        //First thing to check, is if we have our agent connected; that would make things really simple and avoid any need
        //for warnings; this is especially important in some launch modes, specifically the ones in which it would be more
        //difficult for the user to provide explicit configuration as we control them from Quarkus tooling e.g. 'dev-mode'.
        //N.B. this is also our favourite implementation because it allows for better diagnostics and easier debugging.
        Instrumentation existingInstrumentation = ClassChangeAgent.getInstrumentation();
        //Checking this first as it's very cheap:
        if (existingInstrumentation != null) {
            //Intentionally not warning, we'll do a single warning below to not be overwhelming.
            logger.debugf("Initializing AgentBasedModulesReconfigurer from existing instrumentation agent");
            return new AgentBasedModulesReconfigurer(existingInstrumentation);
        }

        //Let's try to see if --add-opens=java.base/java.lang.invoke=ALL-UNNAMED was set, that would give us a great entry point:
        logger.debugf("Attempting to initialize ReflectiveAccessModulesReconfigurer");
        try {
            return new ReflectiveAccessModulesReconfigurer();
        } catch (RuntimeException e) {
            //Intentionally not warning, we'll do a single warning below to not be overwhelming.
            logger.debugf(e, "Failed to initialize ReflectiveAccessModulesReconfigurer");
        }

        //Next let's try the approach based on --add-exports being set on JVM boot:
        logger.debugf("Attempting to initialize DirectExportedModulesAPIModulesReconfigurer");
        try {
            return new DirectExportedModulesAPIModulesReconfigurer();
        } catch (RuntimeException e) {
            //Intentionally not warning, we'll do a single warning below to not be overwhelming.
            logger.debugf(e, "Failed to initialize DirectExportedModulesAPIModulesReconfigurer");
        }

        //ONE actionable warning:
        //N.B. this is the only warning in this complex block, so to provide a single clear direction to the user;
        //the "add-opens=java.base" seems to be our preference, so that's what we suggest setting.
        logger.warn(
                "Could not get access to jdk.internal.module API: this is required for Quarkus to adjust Java Modules configuration to match the various requirements of each extension. Please ensure this JVM is launched with --add-opens=java.base/java.lang.invoke=ALL-UNNAMED.");

        //Last resort when all above failed: let's try via ByteBuddy auto-attach; this WILL trigger JVM warnings (on top of ours) but at least
        //tests will run within the expected behavior of the module system.
        logger.debugf(
                "Attempting to initialize AgentBasedModulesReconfigurer by self-attaching an agent - this will most likely trigger warnings by the JVM, and might fail in future JVM versions");
        try {
            // ByteBuddyAgent.install() attaches its own agent to the current
            // JVM and returns the Instrumentation instance.
            final Instrumentation instrumentation = ByteBuddyAgent.install();
            return new AgentBasedModulesReconfigurer(instrumentation);
        } catch (IllegalStateException e) {
            logger.debugf("ByteBuddy failed to auto-attach", e);
        }

        //At this point, we failed to load a functional strategy - let's at least log which modules should have been reconfigured (but won't):
        return new FallbackModulesReconfigurer();
    }

}
