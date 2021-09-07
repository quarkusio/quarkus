package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * If any build item of this type is produced, the native-image build tool
 * will run with {@literal --allow-incomplete-classpath} set.
 * <p>
 * This should be strongly discouraged as it makes diagnostics of any issue
 * much more complex, and we have it seen affect error message of code
 * seemingly unrelated to the code which is having the broken classpath.
 * <p>
 * Use of this build item will trigger a warning during build.
 *
 * @Deprecated Please don't use it unless there is general consensus that we can't practically find a better solution.
 */
@Deprecated
public final class NativeImageAllowIncompleteClasspathBuildItem extends MultiBuildItem {

    private final String extensionName;

    /**
     * @param extensionName Name the extension requiring this, so that it can be shamed appropriately during build.
     */
    public NativeImageAllowIncompleteClasspathBuildItem(String extensionName) {
        this.extensionName = extensionName;
    }

    public String getExtensionName() {
        return extensionName;
    }
}
