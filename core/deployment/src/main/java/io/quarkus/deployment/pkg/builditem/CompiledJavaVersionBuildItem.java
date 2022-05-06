package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

public final class CompiledJavaVersionBuildItem extends SimpleBuildItem {

    private final JavaVersion javaVersion;

    private CompiledJavaVersionBuildItem(JavaVersion javaVersion) {
        this.javaVersion = javaVersion;
    }

    public static CompiledJavaVersionBuildItem unknown() {
        return new CompiledJavaVersionBuildItem(new JavaVersion.Unknown());
    }

    public static CompiledJavaVersionBuildItem fromMajorJavaVersion(int majorJavaVersion) {
        return new CompiledJavaVersionBuildItem(new JavaVersion.Known(majorJavaVersion));
    }

    public JavaVersion getJavaVersion() {
        return javaVersion;
    }

    public interface JavaVersion {

        Status isJava11OrHigher();

        Status isJava17OrHigher();

        enum Status {
            TRUE,
            FALSE,
            UNKNOWN
        }

        final class Unknown implements JavaVersion {

            public static final Unknown INSTANCE = new Unknown();

            Unknown() {
            }

            @Override
            public Status isJava11OrHigher() {
                return Status.UNKNOWN;
            }

            @Override
            public Status isJava17OrHigher() {
                return Status.UNKNOWN;
            }
        }

        final class Known implements JavaVersion {

            private static final int JAVA_11_MAJOR = 55;
            private static final int JAVA_17_MAJOR = 61;

            private final int determinedMajor;

            Known(int determinedMajor) {
                this.determinedMajor = determinedMajor;
            }

            @Override
            public Status isJava11OrHigher() {
                return getStatus(JAVA_11_MAJOR);
            }

            @Override
            public Status isJava17OrHigher() {
                return getStatus(JAVA_17_MAJOR);
            }

            private Status getStatus(int javaMajor) {
                return determinedMajor >= javaMajor ? Status.TRUE : Status.FALSE;
            }
        }
    }
}
