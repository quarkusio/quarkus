package io.quarkus.arc.runtime;

import java.util.Optional;
import java.util.regex.Pattern;

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

    public static boolean suppressIfPropertyRegex(String propertyName, Pattern pattern, boolean lookupIfMissing) {
        Config config = ConfigProviderResolver.instance().getConfig();
        Optional<String> optionalValue = config.getOptionalValue(propertyName, String.class);
        if (optionalValue.isPresent()) {
            return !pattern.matcher(optionalValue.get()).matches();
        } else {
            return !lookupIfMissing;
        }
    }

    public static boolean suppressUnlessPropertyRegex(String propertyName, Pattern pattern, boolean lookupIfMissing) {
        Config config = ConfigProviderResolver.instance().getConfig();
        Optional<String> optionalValue = config.getOptionalValue(propertyName, String.class);
        if (optionalValue.isPresent()) {
            return pattern.matcher(optionalValue.get()).matches();
        } else {
            return !lookupIfMissing;
        }
    }
}
