package io.quarkus.runtime.configuration;

import java.util.Set;

public class PropertiesUtil {
    private PropertiesUtil() {
    }

    public static boolean isPropertyInRoot(Set<String> roots, NameIterator propertyName) {
        for (String root : roots) {
            // match everything
            if (root.length() == 0) {
                return true;
            }

            // A sub property from a namespace is always bigger in length
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

                // root has no more segments and we reached this far so everything matched.
                // on top, property still has more segments to do the mapping.
                if (!rootNi.hasNext() && propertyName.hasNext()) {
                    propertyName.goToStart();
                    return true;
                }
            }
        }

        return false;
    }
}
