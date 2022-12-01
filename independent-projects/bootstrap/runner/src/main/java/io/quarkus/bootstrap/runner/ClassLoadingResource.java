package io.quarkus.bootstrap.runner;

import java.net.URL;
import java.security.ProtectionDomain;

public interface ClassLoadingResource {

    /**
     * A lifecycle hook that should be called when the ClassLoader to which this resource belongs to
     * is constructed
     */
    void init(ClassLoader runnerClassLoader);

    byte[] getResourceData(String resource);

    URL getResourceURL(String resource);

    ManifestInfo getManifestInfo();

    /**
     * This can only be called after {@code init} has been called
     */
    ProtectionDomain getProtectionDomain();

    void close();

    /**
     * This is an optional hint to release internal caches, if possible.
     * It is different than {@link #close()} as it's possible that
     * this ClassLoadingResource will still be used after this,
     * so it needs to be able to rebuild any lost state in case of need.
     * However one can assume that when this is invoked, there is
     * some reasonable expectation that this resource is no longer going
     * to be necessary.
     */
    default void resetInternalCaches() {
        //no-op
    }
}
