package io.quarkus.bootstrap.classloading;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;

import io.quarkus.paths.ManifestAttributes;

public abstract class AbstractClassPathElement implements ClassPathElement {

    private static final Logger log = Logger.getLogger(AbstractClassPathElement.class);

    private volatile ManifestAttributes manifestAttributes;
    private volatile boolean manifestInitialized = false;

    @Override
    public ManifestAttributes getManifestAttributes() {
        if (manifestInitialized) {
            return manifestAttributes;
        }
        synchronized (this) {
            if (manifestInitialized) {
                return manifestAttributes;
            }
            manifestAttributes = readManifest();
            manifestInitialized = true;
        }
        return manifestAttributes;
    }

    protected ManifestAttributes readManifest() {
        final ClassPathResource mf = getResource("META-INF/MANIFEST.MF");
        if (mf != null) {
            try (InputStream manifestIs = new ByteArrayInputStream(mf.getData())) {
                return ManifestAttributes.of(new Manifest(manifestIs));
            } catch (IOException e) {
                log.warnf("Failed to parse manifest for %s", toString(), e);
            }
        }
        return null;
    }
}
