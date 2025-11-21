package io.quarkus.deployment.jvm;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.deployment.builditem.ModuleOpenBuildItem;
import net.bytebuddy.agent.ByteBuddyAgent;

final class AgentBasedModulesReconfigurer implements JvmModulesReconfigurer {

    private final Instrumentation instrumentation;

    /**
     * Initializes by self-attaching the Byte Buddy agent
     * to get the Instrumentation instance.
     * If an agent cannot be installed, an {@link IllegalStateException} is thrown.
     */
    AgentBasedModulesReconfigurer() {
        // ByteBuddyAgent.install() attaches its own agent to the current
        // JVM and returns the Instrumentation instance.
        try {
            instrumentation = ByteBuddyAgent.install();
        } catch (IllegalStateException e) {
            throw new RuntimeException("Failed to install an agent in the running JVM. Please report this issue.", e);
        }
    }

    @Override
    public void openJavaModules(List<ModuleOpenBuildItem> addOpens) {
        if (addOpens.isEmpty())
            return;
        //We now need to aggregate the list into a differently organized data structure
        HashMap<Module, PerModuleOpenInstructions> aggregateByModule = new HashMap<>();
        for (ModuleOpenBuildItem m : addOpens) {
            Module openedModule = m.openedModule();
            PerModuleOpenInstructions perModuleOpenInstructions = aggregateByModule.computeIfAbsent(openedModule,
                    k -> new PerModuleOpenInstructions());
            Module openingModule = m.openingModule();
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

}
