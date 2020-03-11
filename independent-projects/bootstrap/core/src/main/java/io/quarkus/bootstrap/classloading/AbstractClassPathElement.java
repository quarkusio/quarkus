package io.quarkus.bootstrap.classloading;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;

public abstract class AbstractClassPathElement implements ClassPathElement {

    private static final Logger log = Logger.getLogger(AbstractClassPathElement.class);

    private volatile Manifest manifest;
    private volatile boolean initialized = false;

    @Override
    public Manifest getManifest() {
        if (initialized) {
            return manifest;
        }
        synchronized (this) {
            if (initialized) {
                return manifest;
            }
            ClassPathResource mf = getResource("META-INF/MANIFEST.MF");
            if (mf != null) {
                try {
                    manifest = new Manifest(new ByteArrayInputStream(mf.getData()));
                } catch (IOException e) {
                    log.warnf("Failed to parse manifest for %s", toString());
                }
            }
            initialized = true;
            return manifest;
        }
    }
}
