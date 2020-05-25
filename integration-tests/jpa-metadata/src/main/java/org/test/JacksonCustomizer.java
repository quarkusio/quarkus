package org.test;

import java.util.*;

import javax.inject.*;

import com.fasterxml.jackson.databind.*;

import io.quarkus.hibernate.orm.runtime.*;
import io.quarkus.jackson.*;

/**
 * See <a href="https://quarkus.io/guides/rest-json#jackson">Quarkus Jackson Customization</a>
 */
@Singleton
public class JacksonCustomizer implements ObjectMapperCustomizer {
    private final QuarkusHibernateMetadata metadata;

    private final Set<Class<?>> entities = new HashSet<>();
    private final Set<Class<?>> all = new HashSet<>();
    private final Set<Class<?>> others = new HashSet<>();
    private int called = 0;

    @Inject
    public JacksonCustomizer(QuarkusHibernateMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public void customize(ObjectMapper ignored) {
        this.called++;

        this.all.addAll(metadata.getAllManagedClasses());
        this.entities.addAll(metadata.getEntityClasses());
        this.others.addAll(metadata.getOtherClasses());
    }

    public Set<Class<?>> getEntities() {
        return entities;
    }

    public Set<Class<?>> getAll() {
        return all;
    }

    public Set<Class<?>> getOthers() {
        return others;
    }

    public int customizeCalled() {
        return called;
    }
}
