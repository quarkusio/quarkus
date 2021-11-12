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
        private final String version;
        private final int javaVersion;
        private final String distribution;

        public GraalVMVersion(String fullVersion, String version, int javaVersion, String distribution) {
            this.fullVersion = fullVersion;
            this.version = version;
            this.javaVersion = javaVersion;
            this.distribution = distribution;
        }

        public String getFullVersion() {
            return fullVersion;
        }

        public String getVersion() {
            return version;
        }

        public int getJavaVersion() {
            return javaVersion;
        }

        public String getDistribution() {
            return distribution;
        }
    }
}
