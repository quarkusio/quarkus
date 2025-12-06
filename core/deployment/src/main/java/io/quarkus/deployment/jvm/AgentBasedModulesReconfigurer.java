package io.quarkus.deployment.jvm;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.quarkus.changeagent.ClassChangeAgent;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;
import net.bytebuddy.agent.ByteBuddyAgent;

final class AgentBasedModulesReconfigurer extends AbstractModulesReconfigurer implements JvmModulesReconfigurer {

    private final Instrumentation instrumentation;

    /**
     * Initializes, attempting to find or load an `Instrumentation` instance:
     * first we check if the `ClassChangeAgent` is attached - in which case
     * we can use it.
     * Otherwise we'll proceed to attaching a new agent leveraging the
     * self-attaching strategy from Byte Buddy.
     * If an agent cannot be installed, an {@link IllegalStateException} is thrown.
     */
    AgentBasedModulesReconfigurer() {
        Instrumentation existingInstrumentation = ClassChangeAgent.getInstrumentation();
        if (existingInstrumentation != null) {
            this.instrumentation = existingInstrumentation;
        } else {
            // ByteBuddyAgent.install() attaches its own agent to the current
            // JVM and returns the Instrumentation instance.
            try {
                instrumentation = ByteBuddyAgent.install();
            } catch (IllegalStateException e) {
                throw new RuntimeException("Failed to install an agent in the running JVM. Please report this issue.", e);
            }
        }
        if (logger.isDebugEnabled()) {
            instrumentation.addTransformer(new UnnamedModulesTracker());
        }
    }

    private static void reportUnnamedModulesSet(Instrumentation inst) {
        if (!logger.isDebugEnabled()) {
            //All following work is only useful to emit a comprehensive debugging report
            return;
        }
        Set<Module> unnamedModules = new HashSet<>();

        // Always add the Boot Loader's unnamed module (for -Xbootclasspath/a):
        // there isn't a ClassLoader object for Boot, but we can get it via the Layer.
        unnamedModules.add(ClassLoader.getSystemClassLoader().getUnnamedModule());

        // Iterate all loaded classes to find other ClassLoaders (expensive!)
        Class<?>[] allClasses = inst.getAllLoadedClasses();

        for (Class<?> clazz : allClasses) {
            ClassLoader loader = clazz.getClassLoader();

            // If loader is null, it's the Boot Loader (already handled)
            if (loader != null) {
                Module m = loader.getUnnamedModule();
                unnamedModules.add(m);
            }
        }

        long pid = ProcessHandle.current().pid();
        logger.debugf("Set of unnamed modules currently defined in process with pid=%d: %s", pid, unnamedModules);
    }

    @Override
    public void openJavaModules(List<ModuleOpenBuildItem> addOpens, ModulesClassloaderContext modulesContext) {
        if (addOpens.isEmpty())
            return;
        reportUnnamedModulesSet(this.instrumentation);//Provides very useful diagnostics
        //We now need to aggregate the list into a differently organized data structure
        HashMap<Module, PerModuleOpenInstructions> aggregateByModule = new HashMap<>();
        for (ModuleOpenBuildItem m : addOpens) {
            Optional<Module> openedModuleOptional = modulesContext.findModule(m.openedModuleName());
            if (openedModuleOptional.isEmpty()) {
                warnModuleGetsSkipped(m.openedModuleName(), m);
                continue;
            }
            final Module openedModule = openedModuleOptional.get();
            PerModuleOpenInstructions perModuleOpenInstructions = aggregateByModule.computeIfAbsent(openedModule,
                    k -> new PerModuleOpenInstructions());
            Optional<Module> openingModuleNameOptional = modulesContext.findModule(m.openingModuleName());
            if (openingModuleNameOptional.isEmpty()) {
                warnModuleGetsSkipped(m.openingModuleName(), m);
                continue;
            }
            final Module openingModule = openingModuleNameOptional.get();
            for (String packageName : m.packageNames()) {
                perModuleOpenInstructions.addOpens(packageName, openingModule);
            }
        }
        //Now that we have a map of openings for each module, let's instrument each of them
        for (Map.Entry<Module, PerModuleOpenInstructions> entry : aggregateByModule.entrySet()) {
            addOpens(entry.getKey(), entry.getValue().modulesToOpenToByPackage);
        }
    }

    /**
     * Uses the MethodHandle to open a package.
     *
     * @param sourceModule The module to open
     * @param openInstructions The map of packages / target modules to open to
     */
    private void addOpens(Module sourceModule, Map<String, Set<Module>> openInstructions) {
        if (logger.isDebugEnabled()) {
            openInstructions.forEach(
                    (pkg, modules) -> logger.debugf("Opening package %s of %s to modules %s", pkg, sourceModule, modules));
        }
        try {
            // We are redefining the target module, adding a new "open"
            // rule for it.
            //This method is additive: we don't need to read previous reads and exports
            //to avoid losing them.
            instrumentation.redefineModule(
                    sourceModule, // The module to change
                    Set.of(), // Extra reads
                    Map.of(), // Extra exports
                    openInstructions, // The relevant one
                    Set.of(), // Extra uses
                    Map.of() // Extra provides
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to redefine module " + sourceModule.getName());
        }
    }

    // A convenience container to keep our logic above more readable
    private static class PerModuleOpenInstructions {
        private final Map<String, Set<Module>> modulesToOpenToByPackage = new HashMap<>();

        public void addOpens(final String packageName, final Module openingModule) {
            final Set<Module> modulesToOpenTo = modulesToOpenToByPackage.computeIfAbsent(packageName, k -> new HashSet<>());
            modulesToOpenTo.add(openingModule);
        }
    }

    /**
     * This isn't going to transform any class, but we leverage the existing agent
     * and register as a callback to provide useful diagnostics: we can detect new
     * unnamed modules being created and log them.
     * Obviously this has a cost, so register this only when the matching log level is enabled.
     * N.B. we can use this approach only when having an agent: other implementations
     * of JvmModulesReconfigurer will be more limited.
     */
    private static class UnnamedModulesTracker implements ClassFileTransformer {

        private final Set<Module> knownUnnamedModules = new CopyOnWriteArraySet<>();

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            if (loader != null) {
                Module m = loader.getUnnamedModule();
                // Check if this module is one of the "known" ones from your bootstrap list
                if (knownUnnamedModules.add(m)) {
                    logger.debugf("New unnamed module detected: %s", m);
                }
            }
            return null;
        }
    }
}
