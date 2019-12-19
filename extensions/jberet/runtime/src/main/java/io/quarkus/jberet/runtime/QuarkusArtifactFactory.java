package io.quarkus.jberet.runtime;

import org.jberet.creation.AbstractArtifactFactory;

import io.quarkus.arc.Arc;

class QuarkusArtifactFactory extends AbstractArtifactFactory {

    @Override
    public Object create(String ref, Class<?> cls, ClassLoader classLoader) {
        if (ref != null) {
            return Arc.container().instance(ref).get();
        } else {
            return Arc.container().instance(cls).get();
        }
    }

    @Override
    public Class<?> getArtifactClass(String ref, ClassLoader classLoader) {
        return Arc.container().bean(ref).getBeanClass();
    }
}
