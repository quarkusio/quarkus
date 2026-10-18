package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A package that will be initialized at runtime in native mode.
 * <p>
 * WARNING: this build item should not be used in Quarkus itself and is only provided
 * to simplify the early stages of external extensions development.
 * <p>
 * For Quarkus development, please take the time to surgically mark individual classes as runtime initialized.
 */
public final class RuntimeInitializedPackageBuildItem extends MultiBuildItem {

    private final String packageName;

    public RuntimeInitializedPackageBuildItem(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }
}
