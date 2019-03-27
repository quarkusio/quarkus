package io.quarkus.camel.core.runtime.graal;

import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.config.ConfigProvider;

public final class XmlDisabled implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        String val = ConfigProvider.getConfig().getValue("quarkus.camel.disable-xml", String.class);
        return Boolean.parseBoolean(val);
    }

}
