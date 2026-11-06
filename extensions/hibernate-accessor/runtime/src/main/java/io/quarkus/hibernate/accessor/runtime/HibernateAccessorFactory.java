package io.quarkus.hibernate.accessor.runtime;

import java.util.Map;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.spi.AccessorConfiguration;

public final class HibernateAccessorFactory {

    private HibernateAccessorFactory() {
    }

    public static AccessorFactory createReflectionFactory() {
        // MethodHandles don't work at all in GraalVM 20 and below, and seem unreliable on GraalVM 21,
        // hence the reflection-based strategy rather than one of the method handle based ones.
        return AccessorFactory.reflection(new AccessorConfiguration(new QuarkusAccessContext(), Map.of()));
    }

}
