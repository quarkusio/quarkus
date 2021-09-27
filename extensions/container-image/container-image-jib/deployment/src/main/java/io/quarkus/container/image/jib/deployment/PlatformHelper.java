package io.quarkus.container.image.jib.deployment;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.google.cloud.tools.jib.api.buildplan.Platform;

/*
 * This code is a simplified version of <a href="https://github.com/containerd/containerd/blob/release/1.4/platforms/platforms.go">platforms</a>
 * The reason is to comply with docker's convention and the format is not formally defined in Jib, although it use a reverse notation "arch/os" in documentation
 * 
 * The accepted platform format is: <os>|<arch>[/variant]|<os>/<arch>[/variant]
 * The default OS is linux and the default Architecture is amd64 (as in Jib lib)
 * Valid values for OS and Arch are define by OCI. See <a href="https://github.com/opencontainers/image-spec/blob/main/image-index.md">image-index</a>
 *
*/
final class PlatformHelper {
    private static final Logger log = Logger.getLogger(PlatformHelper.class);

    public static final String OS_DEFAULT = "linux";
    public static final String ARCH_DEFAULT = "amd64";

    private PlatformHelper() {
    }

    private static Optional<String> normalizeOs(String os) {
        os = os.trim().toLowerCase();
        if (os.isEmpty()) {
            return Optional.empty();
        }
        switch (os) {
            case "macos":
                os = "darwin";
                break;
        }
        return Optional.of(os);
    }

    private static boolean isKnownOs(String os) {
        switch (os) {
            case "aix":
            case "android":
            case "darwin":
            case "dragonfly":
            case "freebsd":
            case "hurd":
            case "illumos":
            case "js":
            case "linux":
            case "nacl":
            case "netbsd":
            case "openbsd":
            case "plan9":
            case "solaris":
            case "windows":
            case "zos":
                return true;
        }
        return false;
    }

    private static boolean isKnownArch(String arch) {
        String[] elements = arch.split("/", 2);
        switch (elements[0]) {
            case "386":
            case "amd64":
            case "arm64":
            case "arm":
            case "amd64p32":
            case "armbe":
            case "arm64be":
            case "ppc64":
            case "ppc64le":
            case "mips":
            case "mipsle":
            case "mips64":
            case "mips64le":
            case "mips64p32":
            case "mips64p32le":
                return true;
        }
        return false;
    }

    private static Optional<String> normalizeArch(String arch) {
        String[] elements = arch.split("/", 2);
        if (elements[0].isEmpty()) {
            return Optional.empty();
        }
        Optional<String> variant = elements.length == 1 || elements[1].isEmpty() ? Optional.empty()
                : Optional.of(elements[1].trim().toLowerCase());
        arch = elements[0].trim().toLowerCase();
        switch (arch) {
            case "i386":
                arch = "386";
                break;

            case "x86_64":
            case "x86-64":
                arch = "amd64";
                break;

            case "aarch64":
            case "arm64":
                arch = "arm64";
                if (variant.filter(v -> v.equals("8") || v.equals("v8")).isPresent()) {
                    variant = Optional.empty();
                }
                break;

            case "armhf":
                arch = "arm";
                variant = Optional.of("v7");
                break;

            case "armel":
                arch = "arm";
                variant = Optional.of("v6");
                break;

            case "arm":
                if (!variant.isPresent()) {
                    variant = Optional.of("v7");
                } else if (variant.get().matches("\\d+")) {
                    variant = Optional.of("v" + variant.get());
                }
                break;
        }
        return Optional.of(variant.isPresent() ? arch + "/" + variant.get() : arch);
    }

    public static Platform parse(String specifier) {
        Platform platform;
        String[] elements = specifier.split("/", 2);
        if (elements.length == 1) {
            Optional<String> os = normalizeOs(elements[0]);
            if (os.filter(PlatformHelper::isKnownOs).isPresent()) {
                platform = new Platform(ARCH_DEFAULT, os.get());
            } else {
                platform = new Platform(normalizeArch(elements[0]).orElse(ARCH_DEFAULT), OS_DEFAULT);
            }
        } else {
            Optional<String> arch = normalizeArch(specifier);
            if (arch.filter(PlatformHelper::isKnownArch).isPresent()) {
                platform = new Platform(arch.get(), OS_DEFAULT);
            } else {
                platform = new Platform(normalizeArch(elements[1]).orElse(ARCH_DEFAULT),
                        normalizeOs(elements[0]).orElse(OS_DEFAULT));
            }
        }
        log.debug("Platform Added=" + PlatformHelper.platformToString(platform) + " from specifier: " + specifier);
        return platform;
    }

    public static String platformToString(Platform platform) {
        return platform.getOs() + "/" + platform.getArchitecture();
    }

    public static Set<Platform> parse(Collection<String> platformSpecifiers) {
        return platformSpecifiers
                .stream()
                .map(PlatformHelper::parse)
                .collect(Collectors.toSet());
    }

}
