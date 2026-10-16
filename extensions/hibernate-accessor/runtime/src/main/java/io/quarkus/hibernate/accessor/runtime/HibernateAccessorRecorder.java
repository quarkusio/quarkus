package io.quarkus.hibernate.accessor.runtime;

import java.util.Map;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.spi.AccessorConfiguration;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateAccessorRecorder {

    public void initAccessorImplFactory(String readerClass, String writerClass, String instantiatorClass) {
        AccessorImplFactory.init(readerClass, writerClass, instantiatorClass);
    }

    public RuntimeValue<AccessorFactory> createAccessorFactory(String generatedFactoryClassName) {
        try {
            Class<?> factoryClass = Class.forName(generatedFactoryClassName, true,
                    Thread.currentThread().getContextClassLoader());
            AccessorFactory factory = (AccessorFactory) factoryClass
                    .getMethod("create")
                    .invoke(null);
            AccessorImplFactory.setFactory(factory);
            return new RuntimeValue<>(factory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate generated accessor factory: " + generatedFactoryClassName, e);
        }
    }

    public RuntimeValue<AccessorFactory> createAccessorFactoryWithFallback(String generatedFactoryClassName) {
        try {
            Class<?> factoryClass = Class.forName(generatedFactoryClassName, true,
                    Thread.currentThread().getContextClassLoader());
            AccessorFactory factory = (AccessorFactory) factoryClass
                    .getMethod("create", AccessorFactory.class)
                    .invoke(null, reflectionFactory());
            AccessorImplFactory.setFactory(factory);
            return new RuntimeValue<>(factory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate generated accessor factory with fallback: "
                    + generatedFactoryClassName, e);
        }
    }

    public RuntimeValue<AccessorFactory> createReflectionFactory() {
        AccessorFactory factory = reflectionFactory();
        AccessorImplFactory.setFactory(factory);
        return new RuntimeValue<>(factory);
    }

    private static AccessorFactory reflectionFactory() {
        // MethodHandles don't work at all in GraalVM 20 and below, and seem unreliable on GraalVM 21,
        // hence the reflection-based strategy rather than one of the method handle based ones.
        return AccessorFactory.reflection(new AccessorConfiguration(new QuarkusAccessContext(), Map.of()));
    }

}
