package io.quarkus.arc.runtime;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

public final class SuppressConditions {

    private SuppressConditions() {
    }

    public static boolean suppressIfProperty(String propertyName, String stringValue, boolean lookupIfMissing) {
        Config config = ConfigProviderResolver.instance().getConfig();
        Optional<String> optionalValue = config.getOptionalValue(propertyName, String.class);
        if (optionalValue.isPresent()) {
            return !stringValue.equals(optionalValue.get());
        } else {
            return !lookupIfMissing;
        }
    }

    public static boolean suppressUnlessProperty(String propertyName, String stringValue, boolean lookupIfMissing) {
        Config config = ConfigProviderResolver.instance().getConfig();
        Optional<String> optionalValue = config.getOptionalValue(propertyName, String.class);
        if (optionalValue.isPresent()) {
            return stringValue.equals(optionalValue.get());
        } else {
            return !lookupIfMissing;
        }
    }

}
