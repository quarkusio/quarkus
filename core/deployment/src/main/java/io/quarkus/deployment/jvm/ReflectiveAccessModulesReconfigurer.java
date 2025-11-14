package io.quarkus.deployment.jvm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

/**
 * Implements the {@link JvmModulesReconfigurer} interface to reconfigure JVM module restrictions through reflective access.
 *
 * Restrictions:
 * - This class relies on the JVM option: `--add-opens=java.base/java.lang.invoke=ALL-UNNAMED` to access otherwise
 * sealed private methods and fields of the java.base module. Without this option, reflective access will fail.
 *
 * Design Notes:
 * - Reflection is used to access internal JVM mechanisms, making this implementation dependent on the stability of the
 * relevant internal API. We therefore rely on a multiple of different strategies - this being one of them.
 * - This approach bypasses strict module system rules, enabling dynamic adjustments to module access at runtime:
 * it's meant as a convenience during development, Quarkus does not use such techniques in production mode.
 */
final class ReflectiveAccessModulesReconfigurer implements JvmModulesReconfigurer {

    private static final Logger log = Logger.getLogger(ReflectiveAccessModulesReconfigurer.class);

    private final MethodHandle implAddOpensHandle;

    protected ReflectiveAccessModulesReconfigurer() {
        implAddOpensHandle = methodHandleInit();
    }

    @Override
    public void openJavaModules(List<ModuleOpenBuildItem> addOpens) {
        if (addOpens.isEmpty())
            return;
        for (ModuleOpenBuildItem m : addOpens) {
            Module openedModule = m.openedModule();
            Module openingModule = m.openingModule();
            for (String packageName : m.packageNames()) {
                addOpens(openedModule, packageName, openingModule);
            }
        }
    }

    /**
     * Attempts to get a handle to the private implAddOpens method of Module;
     * this is normally sealed, so it MUST be run with: --add-opens=java.base/java.lang.invoke=ALL-UNNAMED
     * Once we have it, we have full access to reconfigure other modules.
     */
    private static MethodHandle methodHandleInit() {
        final MethodHandle handle;
        try {
            //Get the super-privileged MethodHandles.Lookup instance (IMPL_LOOKUP):
            //this is necessary to access the otherwise sealed private implAddOpens method.
            Field lookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");

            //This setAccessible call is the part that would fail when the java.base module is not opened.
            lookupField.setAccessible(true);

            MethodHandles.Lookup privilegedLookup = (MethodHandles.Lookup) lookupField.get(null);

            //Signature of the method we want to find
            MethodType methodType = MethodType.methodType(void.class, String.class, Module.class);

            //Use the privileged lookup to find the private method
            handle = privilegedLookup.findVirtual(
                    Module.class, // Class to find the method in
                    "implAddOpens", // Name of the private method
                    methodType // Signature of the method
            );

            log.debug("Successfully acquired MethodHandle for implAddOpens.");
            return handle;

        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InaccessibleObjectException e) {
            throw new RuntimeException("Failed to acquire handle to Module#implAddOpens. " +
                    "This must be run with JVM parameter '--add-opens=java.base/java.lang.invoke=ALL-UNNAMED'", e);
        }
    }

    /**
     * Uses the MethodHandle to open a package.
     *
     * @param sourceModule The module to open
     * @param packageName The package to open
     * @param targetModule The module to open to
     */
    private void addOpens(Module sourceModule, String packageName, Module targetModule) {
        try {
            implAddOpensHandle.invokeExact(sourceModule, packageName, targetModule);
            log.debugf("Successfully opened module %s/%s to %s",
                    sourceModule.getName(), packageName, targetModule.isNamed() ? targetModule.getName() : "UNNAMED");
        } catch (Throwable e) {
            // MethodHandle.invokeExact throws Throwable
            throw new RuntimeException("Failed to invoke implAddOpens", e);
        }
    }

}
