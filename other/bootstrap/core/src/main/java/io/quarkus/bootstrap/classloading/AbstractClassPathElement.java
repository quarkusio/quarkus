package io.quarkus.bootstrap.classloading;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.jar.Manifest;
import org.jboss.logging.Logger;

public abstract class AbstractClassPathElement implements ClassPathElement {

    private static final Logger log = Logger.getLogger(AbstractClassPathElement.class);

    private volatile Manifest manifest;
    private volatile boolean manifestInitialized = false;

    @Override
    public Manifest getManifest() {
        if (manifestInitialized) {
            return manifest;
        }
        synchronized (this) {
            if (manifestInitialized) {
                return manifest;
            }
            manifest = readManifest();
            manifestInitialized = true;
        }
        return manifest;
    }

    protected Manifest readManifest() {
        final ClassPathResource mf = getResource("META-INF/MANIFEST.MF");
        if (mf != null) {
            try {
                return new Manifest(new ByteArrayInputStream(mf.getData()));
            } catch (IOException e) {
                log.warnf("Failed to parse manifest for %s", toString(), e);
            }
        }
        return null;
    }
}
