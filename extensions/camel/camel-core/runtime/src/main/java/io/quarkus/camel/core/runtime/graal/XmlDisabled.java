package io.quarkus.camel.core.runtime.graal;

import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.config.ConfigProvider;

public final class XmlDisabled implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        Optional<String> val = ConfigProvider.getConfig().getOptionalValue("quarkus.camel.disable-xml", String.class);
        if (val.isPresent()) {
            return Boolean.parseBoolean(val.get());
        } else {
            return false;
        }
    }

}
