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

    public static String ARCH = System.getProperty("os.arch");

    public enum Os {
        WINDOWS(IS_WINDOWS),
        MAC(IS_MAC),
        LINUX(IS_LINUX),
        NONE(false);

        public final boolean active;

        Os(boolean active) {
            this.active = active;
        }
    }

    public enum Arch {
        AMD64("amd64".equalsIgnoreCase(ARCH)),
        AARCH64("aarch64".equalsIgnoreCase(ARCH)),
        NONE(false);

        public final boolean active;

        Arch(boolean active) {
            this.active = active;
        }
    }

    public final Os os;
    public final Arch arch;
    public final String error;

    public UnsupportedOSBuildItem(Os os, String error) {
        this.os = os;
        this.arch = Arch.NONE;
        this.error = error;
    }

    public UnsupportedOSBuildItem(Arch arch, String error) {
        this.os = Os.NONE;
        this.arch = arch;
        this.error = error;
    }

    public UnsupportedOSBuildItem(Os os, Arch arch, String error) {
        this.os = os;
        this.arch = arch;
        this.error = error;
    }

    public boolean triggerError(boolean isContainerBuild) {
        return
        // When the host OS is unsupported, it could have helped to
        // run in a Linux builder image (e.g. an extension unsupported on Windows).
        ((os.active && !isContainerBuild) ||
        // If Linux is the OS the extension does not support,
        // it fails in a container build regardless the host OS,
        // because we have only Linux based builder images.
                (isContainerBuild && os == Os.LINUX)) ||
        // We don't do cross-compilation, even builder images have to be
        // of the same arch, e.g. aarch64 Mac using aarch64 Linux builder image.
        // So if the arch is unsupported, it fails.
                arch.active;
    }
}
