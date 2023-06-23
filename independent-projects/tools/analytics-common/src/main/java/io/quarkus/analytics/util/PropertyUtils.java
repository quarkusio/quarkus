package io.quarkus.analytics.util;

public class PropertyUtils {

    public static Integer getProperty(String propertyName, int defaultValue) {
        if (propertyName == null) {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        Integer result = Integer.getInteger(propertyName);
        try {
            if (result == null) {
                String stringValue = System.getenv(transformToEnvVarName(propertyName));
                if (stringValue != null) {
                    result = Integer.parseInt(stringValue);
                } else {
                    result = defaultValue;
                }
            }
        } catch (NumberFormatException e) {
            result = defaultValue;
        }
        return result;
    }

    public static String getProperty(String propertyName, String defaultValue) {
        if (propertyName == null) {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        String result = System.getProperty(propertyName);
        try {
            if (result == null) {
                String stringValue = System.getenv(transformToEnvVarName(propertyName));
                if (stringValue != null) {
                    result = stringValue;
                } else {
                    result = defaultValue;
                }
            }
        } catch (NumberFormatException e) {
            result = defaultValue;
        }
        return result;
    }

    public static boolean getProperty(String propertyName, boolean defaultValue) {
        if (propertyName == null) {
            throw new IllegalArgumentException("Property name cannot be null");
        }
        boolean result;
        String systemValue = System.getProperty(propertyName);
        try {
            if (systemValue == null) {
                String envValue = System.getenv(transformToEnvVarName(propertyName));
                if (envValue != null) {
                    result = Boolean.parseBoolean(envValue);
                } else {
                    result = defaultValue;
                }
            } else {
                result = Boolean.parseBoolean(systemValue);
            }
        } catch (NumberFormatException e) {
            result = defaultValue;
        }
        return result;
    }

    private static String transformToEnvVarName(String propertyName) {
        return propertyName.toUpperCase().replace('.', '_');
    }
}
