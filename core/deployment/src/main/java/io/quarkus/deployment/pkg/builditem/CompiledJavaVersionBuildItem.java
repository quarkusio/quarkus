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

        Status isJava21OrHigher();

        Status isJava19OrHigher();

        enum Status {
            TRUE,
            FALSE,
            UNKNOWN
        }

        final class Unknown implements JavaVersion {

            Unknown() {
            }

            @Override
            public Status isJava21OrHigher() {
                return Status.UNKNOWN;
            }

            @Override
            public Status isJava19OrHigher() {
                return Status.UNKNOWN;
            }
        }

        final class Known implements JavaVersion {

            private static final int JAVA_19_MAJOR = 63;
            private static final int JAVA_21_MAJOR = 65;

            private final int determinedMajor;

            Known(int determinedMajor) {
                this.determinedMajor = determinedMajor;
            }

            @Override
            public Status isJava19OrHigher() {
                return higherOrEqualStatus(JAVA_19_MAJOR);
            }

            @Override
            public Status isJava21OrHigher() {
                return higherOrEqualStatus(JAVA_21_MAJOR);
            }

            private Status higherOrEqualStatus(int javaMajor) {
                return determinedMajor >= javaMajor ? Status.TRUE : Status.FALSE;
            }

            private Status equalStatus(int javaMajor) {
                return determinedMajor == javaMajor ? Status.TRUE : Status.FALSE;
            }
        }
    }
}
