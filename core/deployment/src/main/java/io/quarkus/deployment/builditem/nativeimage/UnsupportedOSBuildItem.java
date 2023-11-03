package io.quarkus.deployment.builditem.nativeimage;

import static io.quarkus.dev.console.QuarkusConsole.IS_LINUX;
import static io.quarkus.dev.console.QuarkusConsole.IS_MAC;
import static io.quarkus.dev.console.QuarkusConsole.IS_WINDOWS;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Native-image might not be supported for a particular
 * extension on a given OS or architecture.
 */
public final class UnsupportedOSBuildItem extends MultiBuildItem {
    public enum Os {
        WINDOWS(IS_WINDOWS),
        MAC(IS_MAC),
        LINUX(IS_LINUX);

        public final boolean active;

        Os(boolean active) {
            this.active = active;
        }
    }

    public final Os os;
    public final String error;

    public UnsupportedOSBuildItem(Os os, String error) {
        this.os = os;
        this.error = error;
    }

    public boolean triggerError(boolean isContainerBuild) {
        return
        // When the host OS is unsupported, it could have helped to
        // run in a Linux builder image (e.g. an extension unsupported on Windows).
        (os.active && !isContainerBuild) ||
        // If Linux is the OS the extension does not support,
        // it fails in a container build regardless the host OS,
        // because we have only Linux based builder images.
                (isContainerBuild && os == Os.LINUX);
    }
}
