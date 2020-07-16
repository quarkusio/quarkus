package io.quarkus.utilities;

/**
 * Enum to classify the os.name system property
 */
public enum OS {
    WINDOWS,
    LINUX,
    MAC,
    OTHER;

    public static OS determineOS() {
        final String osName = System.getProperty("os.name").toLowerCase();
        final OS os;
        if (osName.contains("windows")) {
            os = OS.WINDOWS;
        } else if (osName.contains("linux")
                || osName.contains("freebsd")
                || osName.contains("unix")
                || osName.contains("sunos")
                || osName.contains("solaris")
                || osName.contains("aix")) {
            os = OS.LINUX;
        } else if (osName.contains("mac os")) {
            os = OS.MAC;
        } else {
            os = OS.OTHER;
        }

        return os;
    }

    // based on https://github.com/trustin/os-maven-plugin/blob/os-maven-plugin-1.6.2/src/main/java/kr/motd/maven/os/Detector.java
    // by Trustin Heuiseung Lee (ASL 2.0)
    public static String getArchitecture() {
        String arch = System.getProperty("os.arch");
        if (arch.matches("^(x8664|amd64|ia32e|em64t|x64|x86_64)$")) {
            return "x86_64";
        }
        if (arch.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            return "x86_32";
        }
        if (arch.matches("^(ia64w?|itanium64)$")) {
            return "itanium_64";
        }
        if ("ia64n".equals(arch)) {
            return "itanium_32";
        }
        if (arch.matches("^(sparc|sparc32)$")) {
            return "sparc_32";
        }
        if (arch.matches("^(sparcv9|sparc64)$")) {
            return "sparc_64";
        }
        if (arch.matches("^(arm|arm32)$")) {
            return "arm_32";
        }
        if ("aarch64".equals(arch)) {
            return "aarch_64";
        }
        if (arch.matches("^(mips|mips32)$")) {
            return "mips_32";
        }
        if (arch.matches("^(mipsel|mips32el)$")) {
            return "mipsel_32";
        }
        if ("mips64".equals(arch)) {
            return "mips_64";
        }
        if ("mips64el".equals(arch)) {
            return "mipsel_64";
        }
        if (arch.matches("^(ppc|ppc32)$")) {
            return "ppc_32";
        }
        if (arch.matches("^(ppcle|ppc32le)$")) {
            return "ppcle_32";
        }
        if ("ppc64".equals(arch)) {
            return "ppc_64";
        }
        if ("ppc64le".equals(arch)) {
            return "ppcle_64";
        }
        if ("s390".equals(arch)) {
            return "s390_32";
        }
        if ("s390x".equals(arch)) {
            return "s390_64";
        }

        return null;
    }
}
