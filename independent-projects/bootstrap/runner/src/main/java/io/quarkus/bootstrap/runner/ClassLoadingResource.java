package io.quarkus.bootstrap.runner;

import java.net.URL;
import java.security.ProtectionDomain;

public interface ClassLoadingResource {

    byte[] getResourceData(String resource);

    URL getResourceURL(String resource);

    ManifestInfo getManifestInfo();

    ProtectionDomain getProtectionDomain(ClassLoader runnerClassLoader);

    void close();

}
