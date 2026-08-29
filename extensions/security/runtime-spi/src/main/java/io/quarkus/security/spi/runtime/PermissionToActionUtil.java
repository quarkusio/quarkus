package io.quarkus.security.spi.runtime;

public final class PermissionToActionUtil {

    public sealed interface ParsedPermission {
        String name();

        String action();

        default boolean hasAction() {
            return action() != null;
        }
    }

    record ParsedPermissionImpl(String name, String action) implements ParsedPermission {
    }

    private PermissionToActionUtil() {
    }

    public static ParsedPermission parse(String raw) {
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("Permission value must not be empty");
        }

        var name = new StringBuilder();
        var action = new StringBuilder();
        boolean foundSeparator = false;
        char[] chars = raw.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
                case '\\':
                    if (++i == chars.length || chars[i] != ':') {
                        throw new IllegalArgumentException(
                                "Invalid escape sequence in permission value '" + raw
                                        + "': backslash is only allowed before a colon (\\:)");
                    }
                    if (foundSeparator) {
                        action.append(':');
                    } else {
                        name.append(':');
                    }
                    break;
                case ':':
                    if (foundSeparator) {
                        throw new IllegalArgumentException(
                                "Permission value '" + raw
                                        + "' contains more than one unescaped colon separator, use \\: for a literal colon");
                    }
                    foundSeparator = true;
                    break;
                default:
                    if (foundSeparator) {
                        action.append(chars[i]);
                    } else {
                        name.append(chars[i]);
                    }
            }
        }

        if (!foundSeparator) {
            return new ParsedPermissionImpl(name.toString(), null);
        }
        if (name.isEmpty()) {
            return new ParsedPermissionImpl(":" + action, null);
        }
        if (action.isEmpty()) {
            return new ParsedPermissionImpl(name + ":", null);
        }
        return new ParsedPermissionImpl(name.toString(), action.toString());
    }
}
