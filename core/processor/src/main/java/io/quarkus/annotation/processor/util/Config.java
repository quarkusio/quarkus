package io.quarkus.annotation.processor.util;

import io.quarkus.annotation.processor.documentation.config.model.Extension;

public class Config {

    private final Extension extension;
    private final boolean useConfigMapping;
    private final boolean debug;

    public Config(Extension extension, boolean useConfigMapping, boolean debug) {
        this.extension = extension;
        this.useConfigMapping = useConfigMapping;
        this.debug = debug;
    }

    public Extension getExtension() {
        return extension;
    }

    public boolean useConfigMapping() {
        return useConfigMapping;
    }

    public boolean isDebug() {
        return debug;
    }
}
