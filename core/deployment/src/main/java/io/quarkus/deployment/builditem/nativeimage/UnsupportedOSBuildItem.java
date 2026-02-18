package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.cpu.CPU;
import io.smallrye.common.os.OS;

/**
 * Native-image might not be supported for a particular
 * extension on a given OS or architecture.
 */
public final class UnsupportedOSBuildItem extends MultiBuildItem {

    @Deprecated(forRemoval = true, since = "3.26.0")
    public static String ARCH = System.getProperty("os.arch");

    /**
     * @deprecated Use {@link OS} instead
     */
    @Deprecated(forRemoval = true, since = "3.26.0")
    public enum Os {
        WINDOWS(OS.WINDOWS.isCurrent()),
        MAC(OS.MAC.isCurrent()),
        LINUX(OS.LINUX.isCurrent()),
        NONE(false);

        public final boolean active;

        Os(boolean active) {
            this.active = active;
        }
    }

    /**
     * @deprecated Use {@link CPU} instead
     */
    @Deprecated(forRemoval = true, since = "3.26.0")
    public enum Arch {
        AMD64("amd64".equalsIgnoreCase(ARCH)),
        AARCH64("aarch64".equalsIgnoreCase(ARCH)),
        NONE(false);

        public final boolean active;

        Arch(boolean active) {
            this.active = active;
        }
    }

    private final OS os;
    private final CPU cpu;
    private final String error;

    /**
     * @deprecated Use {@link UnsupportedOSBuildItem#UnsupportedOSBuildItem(io.smallrye.common.os.OS, java.lang.String)} instead
     */
    @Deprecated(forRemoval = true, since = "3.26.0")
    public UnsupportedOSBuildItem(Os os, String error) {
        this.os = switch (os) {
            case WINDOWS -> OS.WINDOWS;
            case MAC -> OS.MAC;
            case LINUX -> OS.LINUX;
            case NONE -> null;
        };
        this.cpu = null;
        this.error = error;
    }

    /**
     * @deprecated Use {@link UnsupportedOSBuildItem#UnsupportedOSBuildItem(io.smallrye.common.cpu.CPU, java.lang.String)}
     *             instead
     */
    @Deprecated(forRemoval = true, since = "3.26.0")
    public UnsupportedOSBuildItem(Arch arch, String error) {
        this.os = null;
        this.cpu = switch (arch) {
            case AMD64 -> CPU.x64;
            case AARCH64 -> CPU.aarch64;
            case NONE -> null;
        };
        this.error = error;
    }

    /**
     * @deprecated Use
     *             {@link UnsupportedOSBuildItem#UnsupportedOSBuildItem(io.smallrye.common.os.OS, io.smallrye.common.cpu.CPU, java.lang.String)}
     *             instead
     */
    @Deprecated(forRemoval = true, since = "3.26.0")
    public UnsupportedOSBuildItem(Os os, Arch arch, String error) {
        this.os = switch (os) {
            case WINDOWS -> OS.WINDOWS;
            case MAC -> OS.MAC;
            case LINUX -> OS.LINUX;
            case NONE -> null;
        };
        this.cpu = switch (arch) {
            case AMD64 -> CPU.x64;
            case AARCH64 -> CPU.aarch64;
            case NONE -> null;
        };
        this.error = error;
    }

    public UnsupportedOSBuildItem(OS os, String error) {
        this(os, null, error);
    }

    public UnsupportedOSBuildItem(CPU cpu, String error) {
        this(null, cpu, error);
    }

    public UnsupportedOSBuildItem(OS os, CPU cpu, String error) {
        this.os = os;
        this.cpu = cpu;
        this.error = error;
    }

    public boolean triggerError(boolean isContainerBuild) {
        return
        // When the host OS is unsupported, it could have helped to
        // run in a Linux builder image (e.g. an extension unsupported on Windows).
        ((os != null && os == OS.current() && !isContainerBuild) ||
        // If Linux is the OS the extension does not support,
        // it fails in a container build regardless the host OS,
        // because we have only Linux based builder images.
                (isContainerBuild && os == OS.LINUX)) ||
        // We don't do cross-compilation, even builder images have to be
        // of the same arch, e.g. aarch64 Mac using aarch64 Linux builder image.
        // So if the arch is unsupported, it fails.
                cpu != null && cpu == CPU.host();
    }

    public String error() {
        return error;
    }
}
