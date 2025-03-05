package io.quarkus.runtime.configuration;

import java.util.Set;

import io.smallrye.config.PropertyName;

public class PropertiesUtil {
    private PropertiesUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isMapped(final NameIterator name, final String mapped) {
        int offset = name.getPosition() == -1 ? 0 : name.getPosition() + 1;
        int len = name.getNextEnd() == 0 ? name.getName().length() : name.getNextEnd() - offset;
        return PropertyName.equals(name.getName(), offset, len, mapped, 0, mapped.length());
    }

    public static boolean isPropertyInRoots(final String property, final Set<String> roots) {
        for (String root : roots) {
            if (isPropertyInRoot(property, root)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPropertyInRoot(final String property, final String root) {
        // if property is less than the root no way to match
        if (property.length() < root.length()) {
            return false;
        }

        // if it is the same, then it can still map with parent name
        if (property.equals(root)) {
            return true;
        }

        if (property.length() == root.length()) {
            return false;
        }

        // foo.bar
        // foo.bar."baz"
        // foo.bar[0]
        char c = property.charAt(root.length());
        if ((c == '.') || c == '[') {
            return property.startsWith(root);
        }
        return false;
    }

    public static boolean isPropertyQuarkusCompoundName(NameIterator propertyName) {
        return propertyName.getName().startsWith("\"quarkus.");
    }
}
