package io.quarkus.qute;

import java.util.regex.Pattern;

public final class Namespaces {

    static final Pattern NAMESPACE_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

    private Namespaces() {
    }

    public static boolean isValidNamespace(String value) {
        return NAMESPACE_PATTERN.matcher(value).matches();
    }

    public static String requireValid(String value) {
        if (isValidNamespace(value)) {
            return value;
        }
        throw new TemplateException("[" + value
                + "] is not a valid namespace. The value can only consist of alphanumeric characters and underscores.");
    }

}
