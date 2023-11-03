package io.quarkus.runtime.configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.smallrye.config.KeyMap;

public class PropertiesUtil {
    private PropertiesUtil() {
    }

    /**
     * @deprecated Use {@link PropertiesUtil#filterPropertiesInRoots(Iterable, Set)} instead.
     */
    @Deprecated(forRemoval = true)
    public static boolean isPropertyInRoot(Set<String> roots, NameIterator propertyName) {
        for (String root : roots) {
            // match everything
            if (root.length() == 0) {
                return true;
            }

            // A sub property from a namespace is always bigger
            if (propertyName.getName().length() <= root.length()) {
                continue;
            }

            final NameIterator rootNi = new NameIterator(root);
            // compare segments
            while (rootNi.hasNext()) {
                String segment = rootNi.getNextSegment();
                if (!propertyName.hasNext()) {
                    propertyName.goToStart();
                    break;
                }

                final String nextSegment = propertyName.getNextSegment();
                if (!segment.equals(nextSegment)) {
                    propertyName.goToStart();
                    break;
                }

                rootNi.next();
                propertyName.next();

                // root has no more segments, and we reached this far so everything matched.
                // on top, property still has more segments to do the mapping.
                if (!rootNi.hasNext() && propertyName.hasNext()) {
                    propertyName.goToStart();
                    return true;
                }
            }
        }

        return false;
    }

    public static Iterable<String> filterPropertiesInRoots(final Iterable<String> properties, final Set<String> roots) {
        if (roots.isEmpty()) {
            return properties;
        }

        // Will match everything, so no point in filtering
        if (roots.contains("")) {
            return properties;
        }

        List<String> matchedProperties = new ArrayList<>();
        for (String property : properties) {
            // This is a Quarkus compound name, usually by setting something like `quarkus.foo.bar` in the YAML source
            // TODO - We let it through to match it later again to place it in the right unknown reporting (static or runtime). We can improve this too.
            if (property.startsWith("\"quarkus.")) {
                matchedProperties.add(property);
                continue;
            }

            for (String root : roots) {
                // if property is less than the root no way to match
                if (property.length() < root.length()) {
                    continue;
                }

                // if it is the same, then it can still map with parent name
                if (property.equals(root)) {
                    matchedProperties.add(property);
                    break;
                } else if (property.length() == root.length()) {
                    continue;
                }

                // foo.bar
                // foo.bar."baz"
                // foo.bar[0]
                char c = property.charAt(root.length());
                if ((c == '.') || c == '[') {
                    if (property.startsWith(root)) {
                        matchedProperties.add(property);
                    }
                }
            }
        }
        return matchedProperties;
    }

    public static boolean isPropertyQuarkusCompoundName(NameIterator propertyName) {
        return propertyName.getName().startsWith("\"quarkus.");
    }

    /**
     * Removes false positives of configuration properties marked as unknown. To populate the old @ConfigRoot, all
     * properties are iterated and matched against known roots. With @ConfigMapping the process is different, so
     * properties that are known to @ConfigMapping are not known to the @ConfigRoot, so they will be marked as being
     * unknown. It is a bit easier to just double-check on the unknown properties and remove these false positives by
     * matching them against the known properties of @ConfigMapping.
     *
     * @param unknownProperties the collected unknown properties from the old @ConfigRoot mapping
     * @param filterPatterns the mapped patterns from the discovered @ConfigMapping
     */
    public static void filterUnknown(Set<String> unknownProperties, KeyMap<Boolean> filterPatterns) {
        Set<String> toRemove = new HashSet<>();
        for (String unknownProperty : unknownProperties) {
            if (filterPatterns.hasRootValue(unknownProperty)) {
                toRemove.add(unknownProperty);
            }
        }
        unknownProperties.removeAll(toRemove);
    }
}
