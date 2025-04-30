package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

public final class NativeImageBuildItem extends SimpleBuildItem {

    private final Path path;
    private final GraalVMVersion graalVMVersion;
    private final boolean reused;

    public NativeImageBuildItem(Path path, GraalVMVersion graalVMVersion, boolean reused) {
        this.path = path;
        this.graalVMVersion = graalVMVersion;
        this.reused = reused;
    }

    public Path getPath() {
        return path;
    }

    public GraalVMVersion getGraalVMInfo() {
        return graalVMVersion;
    }

    public boolean isReused() {
        return reused;
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

        public static GraalVMVersion unknown() {
            return new GraalVMVersion("unknown", "unknown", -1, "unknown");
        }

        public Map<String, String> toMap() {
            final Map<String, String> graalVMVersion = new HashMap<>();
            graalVMVersion.put("graalvm.version.full", fullVersion);
            graalVMVersion.put("graalvm.version.version", version);
            graalVMVersion.put("graalvm.version.java", String.valueOf(javaVersion));
            graalVMVersion.put("graalvm.version.distribution", distribution);
            return graalVMVersion;
        }
    }
}
