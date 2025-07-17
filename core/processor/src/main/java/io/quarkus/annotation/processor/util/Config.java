package io.quarkus.annotation.processor.util;

import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.annotation.processor.documentation.config.model.ExtensionModule;

public class Config {

    private final ExtensionModule extensionModule;
    private final boolean debug;

    public Config(ExtensionModule extensionModule, boolean debug) {
        this.extensionModule = extensionModule;
        this.debug = debug;
    }

    public ExtensionModule getExtensionModule() {
        return extensionModule;
    }

    public Extension getExtension() {
        return extensionModule.extension();
    }

    public boolean isDebug() {
        return debug;
    }
}
