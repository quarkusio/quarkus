package io.quarkus.bootstrap.graal;

/**
 * This class essentially contains the same code as {@code org.graalvm.nativeimage.ImageInfo}
 * but we copy it here as we don't want to have to depend {@code org.graalvm.sdk:graal-sdk}
 * and make it a runtimeParentFirst artifact
 */
public final class ImageInfo {

    public static boolean inImageRuntimeCode() {
        return "runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"));
    }

    public static boolean inImageBuildtimeCode() {
        return "buildtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"));
    }
}
