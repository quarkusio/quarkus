package io.quarkus.hibernate.accessor.runtime;

import java.util.Map;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.spi.AccessorConfiguration;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateAccessorRecorder {

    public RuntimeValue<AccessorFactory> createAccessorFactory(RuntimeValue<AccessorFactoryBootstrap> bootstrap) {
        return new RuntimeValue<>(bootstrap.getValue().create(null));
    }

    public RuntimeValue<AccessorFactory> createAccessorFactoryWithFallback(RuntimeValue<AccessorFactoryBootstrap> bootstrap) {
        return new RuntimeValue<>(bootstrap.getValue().create(reflectionFactory()));
    }

    public RuntimeValue<AccessorFactory> createReflectionFactory() {
        AccessorFactory factory = reflectionFactory();
        return new RuntimeValue<>(factory);
    }

    private static AccessorFactory reflectionFactory() {
        // MethodHandles don't work at all in GraalVM 20 and below, and seem unreliable on GraalVM 21,
        // hence the reflection-based strategy rather than one of the method handle based ones.
        return AccessorFactory.reflection(new AccessorConfiguration(new QuarkusAccessContext(), Map.of()));
    }

}
