package org.jboss.resteasy.reactive.common.util;

import java.util.Comparator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class CaseInsensitiveMap<V> extends MultivaluedTreeMap<String, V> {
    public static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();

    private static class CaseInsensitiveComparator
            implements Comparator<String>, java.io.Serializable {

        public int compare(String s1, String s2) {
            if (s1 == s2)
                return 0;
            int n1 = 0;
            // null check is different than JDK version of this method
            if (s1 != null)
                n1 = s1.length();
            int n2 = 0;
            if (s2 != null)
                n2 = s2.length();
            int min = Math.min(n1, n2);
            for (int i = 0; i < min; i++) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(i);
                if (c1 != c2) {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                    if (c1 != c2) {
                        // No overflow because of numeric promotion
                        return c1 - c2;
                    }
                }
            }
            return n1 - n2;
        }
    }

    public CaseInsensitiveMap() {
        super(CASE_INSENSITIVE_ORDER);
    }

}
