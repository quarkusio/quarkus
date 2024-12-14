package io.quarkus.info;

/**
 * This interface provides information about the Java runtime.
 *
 * @see io.quarkus.info.runtime.InfoRecorder
 * @see io.quarkus.info.runtime.JavaInfoContributor
 */
public interface JavaInfo {

    /**
     * Return the Java version with which application compiled.
     *
     * @return string that represent the Java version
     */
    String version();

    /**
     * Return the Java vendor with which application compiled.
     *
     * @return string that represent the Java vendor
     */
    String vendor();

    /**
     * Return the Java vendor version with which application compiled.
     *
     * @return string that represent the Java vendor version
     */
    String vendorVersion();
}
