package io.quarkus.runtime.configuration;

import java.util.HashSet;
import java.util.Set;

import io.smallrye.config.KeyMap;

public class PropertiesUtil {
    private PropertiesUtil() {
    }

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

    public static boolean isPropertyQuarkusCompoundName(NameIterator propertyName) {
        if (propertyName.hasNext()) {
            return propertyName.getNextSegment().startsWith("quarkus.");
        }
        return false;
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
