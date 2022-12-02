package io.quarkus.undertow.deployment;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.metadata.property.PropertyResolver;

public class MPConfigPropertyResolver implements PropertyResolver {

    @Override
    public String resolve(String propertyName) {
        Optional<String> val = ConfigProvider.getConfig().getOptionalValue(propertyName, String.class);
        return val.orElse(null);
    }
}
