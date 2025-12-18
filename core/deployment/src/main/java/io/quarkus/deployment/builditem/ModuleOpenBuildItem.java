package io.quarkus.deployment.builditem;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * This will generate the equivalent of "--add-opens=[openedModule/package(s)]=[openingModule]" for
 * all runners of the generated application.
 * Some limitations apply which will depend on the launch mode of Quarkus; specifically, when
 * generating a runnable Jar we can only open a module to ALL-UNNAMED; this is considered
 * acceptable at the time of writing as extensions are generally placed on the classpath for
 * both fast-jar and uber-jar packaging formats. We expect this to evolve as further packaging
 * formats are introduced which would better leverage the module system.
 * We specifically don't allow opening a module to "ALL-UNNAMED" explicitly to encourage using the module
 * names that a library has or will have in the near future: for this reason, when a module name
 * is provided which doesn't exist, we map it to the unnamed module.
 * This should be treated as an implementation detail of the current packaging format and is most
 * likely bound to evolve.
 * When running integration tests, we aim to run the tests within module access rules consistent with
 * the production packaging modes, but as we need to dynamically modify the module system's access
 * restrictions we need to apply these rules to specific, targeted classloaders: we'll only instrument
 * the unnamed module relating to the Quarkus app classloader. This should be effective but is based
 * on the general understanding that libraries within Quarkus shouldn't use custom classloaders;
 * should this assumption be violated, there's the risk of a "module opens" instruction to not be
 * applied automatically, and users would be required to issue the matching command line flags.
 */
public final class ModuleOpenBuildItem extends MultiBuildItem {

    /**
     * This represents the name of what people refer to as the "unnamed module" in the JVM.
     * We should strictly avoid extensions attempting to refer to the unnamed modules
     * (yes technically there's multiple: each classloader comes with one)
     * so to encourage future-proofing of code using this API.
     */
    private static final String ALL_UNNAMED = "ALL-UNNAMED";

    private final String openedModuleName;
    private final String openingModuleName;
    private final Set<String> packageNames;

    /**
     * Create a new ModuleOpenBuildItem instance.
     * We are currently imposing a strict expectation to not refer to the unnamed module
     * as an opening module so to encourage extension maintainers to pick the right module name
     * in preparation for more extensive modularization.
     * If the library to which you're opening to isn't on the module path yet at this point, it
     * should still be beneficial to refer to it by the expected module name.
     * When a module name is specified which couldn't be identified by name, we'll fallback
     * the opening module name to the unnamed module to allow for an easier transition.
     *
     * @param openedModuleName The name of the module which needs to be opened.
     * @param openingModuleName The name of the module which is being granted access.
     * @param packageNames The packages which are being opened. At least one must be specified.
     */
    public ModuleOpenBuildItem(String openedModuleName, String openingModuleName, String... packageNames) {
        this.openedModuleName = Assert.checkNotEmptyParam("openedModuleName", openedModuleName);
        this.openingModuleName = Assert.checkNotEmptyParam("openingModuleName", openingModuleName);
        this.packageNames = Assert.checkNotEmptyParam("packageNames", Set.of(packageNames));
        if (packageNames.length == 0) {
            throw new IllegalArgumentException("At least one package name must be specified");
        }
        if (ALL_UNNAMED.equals(openedModuleName)) {
            throw new IllegalArgumentException("The unnamed module cannot be opened to other modules");
        }
        if (ALL_UNNAMED.equals(openingModuleName)) {
            throw new IllegalArgumentException(
                    "The unnamed module cannot be used as an opening module identifier: please read the Javadocs for more details");
        }
    }

    public String openedModuleName() {
        return openedModuleName;
    }

    public String openingModuleName() {
        return openingModuleName;
    }

    public Set<String> packageNames() {
        return packageNames;
    }

    @Override
    public String toString() {
        //Used for diagnostic logging purposes
        return "ModuleOpenBuildItem{" +
                "openedModule='" + openedModuleName + '\'' +
                ", openingModule='" + openingModuleName + '\'' +
                ", packages=" + packageNames +
                '}';
    }

}
