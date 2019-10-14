package io.quarkus.platform.tools;

public class ToolsUtils {

    public static String requireProperty(String name) {
        final String value = getProperty(name);
        if(value == null) {
            throw new IllegalStateException("Failed to resolve required property " + name);
        }
        return value;
    }

    public static String getProperty(String name) {
        return getProperty(name, null);
    }

    public static String getProperty(String name, String defaultValue) {
        return System.getProperty(name, defaultValue);
    }
}
