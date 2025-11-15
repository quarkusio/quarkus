package io.quarkus.utilities;

import io.smallrye.common.cpu.CPU;

/**
 * Enum to classify the os.name system property
 *
 * @deprecated Use {@link io.smallrye.common.os.OS} and {@link io.smallrye.common.cpu.CPU} instead.
 */
@Deprecated(forRemoval = true, since = "3.25")
public enum OS {
    WINDOWS,
    LINUX,
    MAC,
    OTHER;

    /**
     * {@return the current OS (not {@code null})}
     *
     * @deprecated Use {@link io.smallrye.common.os.OS#current} instead.
     */
    @Deprecated(forRemoval = true, since = "3.25")
    public static OS determineOS() {
        return switch (io.smallrye.common.os.OS.current()) {
            case WINDOWS -> WINDOWS;
            case LINUX -> LINUX;
            case MAC -> MAC;
            default -> OTHER;
        };
    }

    /**
     * Get a non-standard string for the current architecture.
     * <b>NOTE: {@link CPU} should be used instead of these non-standard strings.</b>
     *
     * @return the string, or {@code null} if the architecture is unknown
     * @deprecated Use {@link io.smallrye.common.cpu.CPU#host} instead (but beware of differing string values).
     */
    @Deprecated(forRemoval = true, since = "3.25")
    public static String getArchitecture() {
        return switch (CPU.host()) {
            case x64 -> "x86_64";
            case x86 -> "x86_32";
            case arm -> "arm_32";
            case aarch64 -> "aarch_64";
            case mips -> "mips_32";
            case mipsel -> "mipsel_32";
            case mips64 -> "mips_64";
            case mips64el -> "mipsel_64";
            case ppc32 -> "ppc_32";
            case ppc32le -> "ppcle_32";
            case ppc -> "ppc_64";
            case ppcle -> "ppcle_64";
            default -> null;
        };
    }
}
