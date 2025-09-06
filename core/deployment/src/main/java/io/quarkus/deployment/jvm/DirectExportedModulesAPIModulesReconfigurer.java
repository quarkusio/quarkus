package io.quarkus.deployment.jvm;

import java.util.List;
import java.util.Optional;

import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

/**
 * A concrete implementation of {@link JvmModulesReconfigurer} designed to reconfigure
 * JVM module restrictions using the direct API calls available in `jdk.internal.module`.
 * Methods in this package are normally not accessible: to compile this class we need to
 * declare the export --add-exports=java.base/jdk.internal.module=ALL-UNNAMED to javac,
 * and this same export is also required at runtime; otherwise an 'java.lang.IllegalAccessError'
 * will be thrown when these methods are invoked.
 */
final class DirectExportedModulesAPIModulesReconfigurer extends AbstractModulesReconfigurer implements JvmModulesReconfigurer {

    DirectExportedModulesAPIModulesReconfigurer() {
        //We need to throw a RuntimeException at construction time if the openJavaModules method is going to fail,
        //so to give the system a chance to try a different strategy when this one won't work.
        testMethodAccessOrFail();
    }

    @Override
    public void openJavaModules(List<ModuleOpenBuildItem> addOpens, ModulesClassloaderContext modulesContext) {
        for (ModuleOpenBuildItem e : addOpens) {
            Optional<Module> optionalOpenedModule = modulesContext.findModule(e.openedModuleName());
            if (optionalOpenedModule.isEmpty()) {
                warnModuleGetsSkipped(e.openedModuleName(), e);
                continue;
            }
            final Module openedModule = optionalOpenedModule.get();
            Optional<Module> optionalOpeningModule = modulesContext.findModule(e.openingModuleName());
            if (optionalOpeningModule.isEmpty()) {
                warnModuleGetsSkipped(e.openingModuleName(), e);
                continue;
            }
            final Module openingModule = optionalOpeningModule.get();
            for (String packageName : e.packageNames()) {
                jdk.internal.module.Modules.addOpens(openedModule, packageName, openingModule);
            }
        }
    }

    /**
     * This method will throw a RuntimeException if we're not allowed to invoke methods on the internal API:
     * useful to validate this particular approach.
     */
    private static void testMethodAccessOrFail() {
        // "null" is not a valid argument for the following method: we use it intentionally to check for being allowed to invoke it,
        // without actually modifying any module.
        try {
            jdk.internal.module.Modules.addOpens(null, null, null);
        } catch (NullPointerException e) {
            //This suggests the method is working as intended: all good!
            //We can return and finish the initialization of this valid instance.
        } catch (java.lang.IllegalAccessError e) {
            //This suggests we're not allowed to invoke it: we need to use a different strategy.
            throw new RuntimeException(
                    "Failed to invoke jdk.internal.module.Modules.addOpens: --add-exports=java.base/jdk.internal.module=ALL-UNNAMED is required, but wasn't set.",
                    e);
        }
    }

}
