package io.quarkus.hibernate.orm.runtime;

import java.util.*;

import javax.enterprise.context.*;

import org.hibernate.internal.*;
import org.jboss.logging.*;

import static java.util.Objects.*;

@ApplicationScoped
public class QuarkusHibernateMetadata {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
            QuarkusHibernateMetadata.class.getName());

    private static Set<Class<?>> allManagedClasses = null;
    private static Set<Class<?>> entityClasses = null;
    private static Set<Class<?>> otherClasses = null;

    static void boot(
            Collection<Class<?>> bootEntityClasses,
            Collection<Class<?>> bootAllClasses) {
        // FYI: this might be called more than once depending on how the hibernate-orm extension recorder is invoked!

        allManagedClasses = Collections.unmodifiableSet(new HashSet<>(bootAllClasses));
        entityClasses = Collections.unmodifiableSet(new HashSet<>(bootEntityClasses));

        Set<Class<?>> others = new HashSet<>(allManagedClasses);
        others.removeAll(entityClasses);
        otherClasses = Collections.unmodifiableSet(others);
    }

    /**
     * Return all classes managed by the EntityManager (i.e.: Entities, Embeddables, ...).
     */
    public Set<Class<?>> getAllManagedClasses() {
        return requireNonNull(allManagedClasses, "Not initialized?");
    }

    /**
     * Return all entity classes managed by the EntityManager.
     */
    public Set<Class<?>> getEntityClasses() {
        return requireNonNull(entityClasses, "Not initialized?");
    }

    /**
     * Return all managed classes <strong>except</strong> entities (i.e.: Embeddables, ...).
     */
    public Set<Class<?>> getOtherClasses() {
        return requireNonNull(otherClasses, "Not initialized?");
    }

}
