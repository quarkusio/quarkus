package io.quarkus.flyway.runtime;

import java.util.Collection;
import java.util.Collections;

import org.flywaydb.core.api.ClassProvider;

public class QuarkusFlywayClassProvider<I> implements ClassProvider<I> {

    private final Collection<Class<? extends I>> classes;

    public QuarkusFlywayClassProvider(Collection<Class<? extends I>> classes) {
        this.classes = Collections.unmodifiableCollection(classes);
    }

    @Override
    public Collection<Class<? extends I>> getClasses() {
        return classes;
    }
}
