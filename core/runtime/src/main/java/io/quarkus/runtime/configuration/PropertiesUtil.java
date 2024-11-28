package io.quarkus.runtime.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.smallrye.config.PropertyName;

public class PropertiesUtil {
    private PropertiesUtil() {
        throw new IllegalStateException("Utility class");
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

    public static Set<PropertyName> toPropertyNames(final Set<String> names) {
        Map<PropertyName, String> propertyNames = new HashMap<>();
        for (String name : names) {
            PropertyName propertyName = new PropertyName(name);
            if (propertyNames.containsKey(propertyName)) {
                String existing = propertyNames.remove(propertyName);
                if (existing.length() < name.length()) {
                    propertyNames.put(new PropertyName(existing), existing);
                } else if (existing.length() > name.length()) {
                    propertyNames.put(propertyName, name);
                } else {
                    if (existing.indexOf('*') <= name.indexOf('*')) {
                        propertyNames.put(new PropertyName(existing), existing);
                    } else {
                        propertyNames.put(propertyName, name);
                    }
                }
            } else {
                propertyNames.put(propertyName, name);
            }
        }
        return propertyNames.keySet();
    }
}
