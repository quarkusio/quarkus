package io.quarkus.hibernate.orm.runtime;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.hibernate.orm.PersistenceUnitMetadata;

import static java.util.Collections.unmodifiableSet;

public class DefaultPersistenceUnitMetadata implements PersistenceUnitMetadata {
    private final Set<String> classNames;

    // poor man's implementation of lazy loading: ConcurrentHashMap with only one entry
    private static final Integer DUMMY_KEY = 0;
    private final Map<Integer, Set<Class<?>>> resolvedClasses = new ConcurrentHashMap<>();

    DefaultPersistenceUnitMetadata(Set<String> classNames) {
        this.classNames = unmodifiableSet(classNames);
    }

    @Override
    public Set<String> getEntityClassNames() {
        return classNames;
    }

    @Override
    public Set<Class<?>> resolveEntityClasses() {
        return resolvedClasses.computeIfAbsent(DUMMY_KEY, ignored -> buildPersistenceUnitMetadata(classNames));
    }

    private Set<Class<?>> buildPersistenceUnitMetadata(Set<String> classNames) {
        Set<Class<?>> classes = new HashSet<>();
        for (String className : classNames) {
            classes.add(classForName(className));
        }

        return unmodifiableSet(classes);
    }

    private Class<?> classForName(String className) {
        try {
            // Explicitly do not initialize classes (i.e.: invoke static initializers) here.
            // => delay this until the application acutally uses/invokes the managed class.
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    "Could not load class: " + className + " using current threads ContextClassLoader", e);
        }
    }

}
