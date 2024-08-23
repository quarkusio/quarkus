package io.quarkus.runtime;

import io.quarkus.bootstrap.graal.ImageInfo;

/**
 * The image execution mode of the application.
 */
public enum ImageMode {
    /**
     * The image mode which indicates that the application is running in a standard JVM.
     */
    JVM,
    /**
     * The image mode which indicates that the application is currently executing the build phase of a native static image.
     */
    NATIVE_BUILD,
    /**
     * The image mode which indicates that the application is a native static image which is currently running on a target
     * system.
     */
    NATIVE_RUN,
    ;

    /**
     * Determine whether the application image is a native static image.
     *
     * @return {@code true} if the application image is a native static image, or {@code false} otherwise
     */
    public boolean isNativeImage() {
        return current() != JVM;
    }

    /**
     * Get the current image mode. Note that it is possible for the image mode to change during the lifetime of
     * an application.
     *
     * @return the image mode (not {@code null})
     */
    public static ImageMode current() {
        if (ImageInfo.inImageBuildtimeCode()) {
            return NATIVE_BUILD;
        } else if (ImageInfo.inImageRuntimeCode()) {
            return NATIVE_RUN;
        } else {
            return JVM;
        }
    }
}
