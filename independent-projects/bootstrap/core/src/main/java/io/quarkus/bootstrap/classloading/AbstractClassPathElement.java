package io.quarkus.bootstrap.classloading;

import io.quarkus.paths.ManifestAttributes;

public abstract class AbstractClassPathElement implements ClassPathElement {

    @Override
    public ManifestAttributes getManifestAttributes() {
        return null;
    }
}
