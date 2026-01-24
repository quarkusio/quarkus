package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * This will generate the equivalent of "--enable-native-access moduleName" for
 * all runners of the generated application.
 * This is being introduced currently to have the API in place, however it's currently very limited:
 * when generating a runnable Jar we can only generate an Enable-Native-Access entry in the Manifest
 * for the modules normally identified by ALL-UNNAMED.
 * This is considered acceptable at the time of writing as extensions are generally placed on the
 * classpath for both fast-jar and uber-jar packaging formats. We expect this to evolve as further packaging
 * formats are introduced which would better leverage the module system.
 * We specifically don't allow enabling native access for "ALL-UNNAMED" explicitly while using this API to
 * encourage using the module names that a library has or will have in the near future:
 * for this reason, when a module name is provided which doesn't exist, we map it to the unnamed module.
 * It is not possible to allow native code access to other modules via an agent, other approaches
 * will need to be identified to reconfigure at runtime.
 */
public final class ModuleEnableNativeAccessBuildItem extends MultiBuildItem {

    private final String moduleName;

    public ModuleEnableNativeAccessBuildItem(final String moduleName) {
        this.moduleName = Assert.checkNotEmptyParam("moduleName", moduleName);
        if (ModuleOpenBuildItem.ALL_UNNAMED.equals(moduleName)) {
            throw new IllegalArgumentException(
                    "You're not expected to use the unnamed module as identifier - please check the javadoc");
        }
    }

    public String moduleName() {
        return moduleName;
    }
}
