package io.quarkus.bootstrap.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

/**
 *
 * @author Alexey Loubyansky
 */
public class PropertyUtils {

    private static final String OS_NAME = "os.name";
    private static final String USER_HOME = "user.home";
    private static final String WINDOWS = "windows";

    private static final String FALSE = "false";
    private static final String TRUE = "true";

    private PropertyUtils() {
    }

    public static boolean isWindows() {
        return getProperty(OS_NAME).toLowerCase(Locale.ENGLISH).contains(WINDOWS);
    }

    public static String getUserHome() {
        return getProperty(USER_HOME);
    }

    public static String getProperty(final String name, String defValue) {
        assert name != null : "name is null";
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(name, defValue);
                }
            });
        } else {
            return System.getProperty(name, defValue);
        }
    }

    public static String getProperty(final String name) {
        assert name != null : "name is null";
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(name);
                }
            });
        } else {
            return System.getProperty(name);
        }
    }

    public static final Boolean getBooleanOrNull(String name) {
        final String value = getProperty(name);
        return value == null ? null : Boolean.parseBoolean(value);
    }

    public static final boolean getBoolean(String name, boolean notFoundValue) {
        final String value = getProperty(name, (notFoundValue ? TRUE : FALSE));
        return value.isEmpty() ? true : Boolean.parseBoolean(value);
    }
}
