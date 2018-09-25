package org.jboss.shamrock.jpa;

import java.util.Set;

/**
 * Internal model to represent which objects are likely needing enhancement
 * via HibernateEntityEnhancer
 */
public interface KnownDomainObjects {

    /**
     * @param className
     * @return false only when it is safe to skip enhancement on the named class.
     */
    boolean contains(String className);

    Set<String> getClassNames();
}
