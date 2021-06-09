package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

public final class NativeImageBuildItem extends SimpleBuildItem {

    private final Path path;
    private final GraalVMVersion graalVMVersion;

    public NativeImageBuildItem(Path path, GraalVMVersion graalVMVersion) {
        this.path = path;
        this.graalVMVersion = graalVMVersion;
    }

    public Path getPath() {
        return path;
    }

    public GraalVMVersion getGraalVMInfo() {
        return graalVMVersion;
    }

    public static class GraalVMVersion {
        private final String fullVersion;
        private final int major;
        private final int minor;
        private final String distribution;

        public GraalVMVersion(String fullVersion, int major, int minor, String distribution) {
            this.fullVersion = fullVersion;
            this.major = major;
            this.minor = minor;
            this.distribution = distribution;
        }

        public String getFullVersion() {
            return fullVersion;
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public String getDistribution() {
            return distribution;
        }
    }
}
