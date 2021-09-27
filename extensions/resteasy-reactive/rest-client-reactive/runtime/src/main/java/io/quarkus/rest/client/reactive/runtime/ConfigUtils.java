package io.quarkus.rest.client.reactive.runtime;

import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

public class ConfigUtils {

    private static final Logger log = Logger.getLogger(ConfigUtils.class);

    public static String getConfigValue(String configProperty, boolean required) {
        String propertyName = stripPrefixAndSuffix(configProperty);
        try {
            return ConfigProvider.getConfig().getValue(propertyName, String.class);
        } catch (NoSuchElementException e) {
            String message = "Failed to find value for config property " + configProperty +
                    " in application configuration. Please provide the value for the property, e.g. by adding " +
                    propertyName + "=<desired-value> to your application.properties";
            if (required) {
                throw new IllegalArgumentException(message, e);
            } else {
                log.warn(message);
                return null;
            }
        } catch (IllegalArgumentException e) {
            String message = "Failed to convert value for property " + configProperty + " to String";
            if (required) {
                throw new IllegalArgumentException(message, e);
            } else {
                log.warn(message);
                return null;
            }
        }
    }

    private static String stripPrefixAndSuffix(String configProperty) {
        // by now we know that configProperty is of form ${...}
        return configProperty.substring(2, configProperty.length() - 1);
    }
}
