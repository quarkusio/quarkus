package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * All timezones are now included in native executables by default so this build item does not change the build behavior
 * anymore.
 * <p>
 * Keeping it around for now as it marks the extension requiring all the timezones
 * and better wait for the situation to fully settle on the GraalVM side
 * before removing it entirely.
 */
@Deprecated
public final class NativeImageEnableAllTimeZonesBuildItem extends MultiBuildItem {

}
