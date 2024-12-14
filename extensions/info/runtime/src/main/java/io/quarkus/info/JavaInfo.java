package io.quarkus.info;

/**
 * This interface provides information about the Java runtime.
 *
 * @see io.quarkus.info.runtime.InfoRecorder
 * @see io.quarkus.info.runtime.JavaInfoContributor
 */
public interface JavaInfo {

    /**
     * Return the Java runtime version.
     *
     * @return string that represent the Java version
     */
    String version();

    /**
     * Return the Java vendor.
     *
     * @return string that represent the Java vendor
     */
    String vendor();

    /**
     * Return the Java vendor runtime version.
     *
     * @return string that represent the Java vendor version
     */
    String vendorVersion();
}
