package io.quarkus.rest.client.reactive.runtime;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

public class ConfigUtils {

    private static final Logger log = Logger.getLogger(ConfigUtils.class);

    static final String PREFIX = "${";
    static final String SUFFIX = "}";

    /**
     * Interpolates the given expression. The expression is expected to be in the form of ${config.property.name}.
     *
     * @param expression the expression to interpolate
     * @param required whether the expression is required to be present in the configuration
     * @return null if the resulting expression is empty, otherwise the interpolated expression
     */
    @SuppressWarnings("unused")
    public static String interpolate(String expression, boolean required) {
        StringBuilder sb = new StringBuilder(expression);
        int idx;
        while ((idx = sb.lastIndexOf(PREFIX)) > -1) {
            int endIdx = sb.indexOf(SUFFIX, idx);
            if (endIdx < 0) {
                throw new IllegalArgumentException("Invalid expression: " + expression);
            }
            String configValue = getConfigValue(sb.substring(idx, endIdx + 1), required);
            // If no value is found, we return null directly
            if (configValue == null) {
                return null;
            }
            sb.replace(idx, endIdx + 1, configValue);
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    /**
     * Obtains the value of the {@param configProperty} expression. This expressions MUST be in the form of ${...}
     */
    public static String getConfigValue(String configProperty, boolean required) {
        return doGetConfigValue(configProperty, required, stripPrefixAndSuffix(configProperty));
    }

    /**
     * Obtains the value of the {@param propertyName} name, meaning that the name must NOT start with '${' or end with '}'
     */
    public static String doGetConfigValue(String configPropertyName, boolean required, String propertyName) {
        try {
            Optional<String> optionalValue = ConfigProvider.getConfig().getOptionalValue(propertyName, String.class);
            if (optionalValue.isEmpty()) {
                String message = String.format("Failed to find value for config property %s in application configuration. "
                        + "Please provide the value for the property, e.g. by adding %s=<desired-value> to your application.properties",
                        configPropertyName, propertyName);
                if (required) {
                    throw new IllegalArgumentException(message);
                }
                log.warn(message);
            }
            return optionalValue.orElse(null);
        } catch (IllegalArgumentException e) {
            String message = "Failed to convert value for property " + configPropertyName + " to String";
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
