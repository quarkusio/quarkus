package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;

class BeanContainerImpl implements BeanContainer {

    private static final Logger LOGGER = Logger.getLogger(BeanContainerImpl.class.getName());

    private final ArcContainer container;

    BeanContainerImpl(ArcContainer container) {
        this.container = container;
    }

    @Override
    public <T> T beanInstance(Class<T> beanType, Annotation... beanQualifiers) {
        return container.select(beanType, beanQualifiers).get();
    }

    @Override
    public <T> Factory<T> beanInstanceFactory(Class<T> type, Annotation... qualifiers) {
        Supplier<InstanceHandle<T>> handleSupplier = container.beanInstanceSupplier(type, qualifiers);
        return createFactory(handleSupplier, null, type, qualifiers);
    }

    @Override
    public <T> Factory<T> beanInstanceFactory(Supplier<Factory<T>> fallbackSupplier, Class<T> type,
            Annotation... qualifiers) {
        Supplier<InstanceHandle<T>> handleSupplier = container.beanInstanceSupplier(type, qualifiers);
        return createFactory(handleSupplier, fallbackSupplier, type, qualifiers);
    }

    private <T> Factory<T> createFactory(Supplier<InstanceHandle<T>> handleSupplier, Supplier<Factory<T>> fallbackSupplier,
            Class<T> type, Annotation... qualifiers) {
        if (handleSupplier == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf(
                        "No matching bean found for type %s and qualifiers %s. The bean might have been marked as unused and removed during build.",
                        type, Arrays.toString(qualifiers));
            }
            if (fallbackSupplier != null) {
                return fallbackSupplier.get();
            } else {
                // by default, if there is no bean, return factory that tries to instantiate non-cdi object
                return new DefaultInstanceFactory<>(type);
            }
        }
        return new Factory<T>() {
            @Override
            public Instance<T> create() {
                InstanceHandle<T> handle = handleSupplier.get();
                return new Instance<T>() {
                    @Override
                    public T get() {
                        return handle.get();
                    }

                    @Override
                    public void close() {
                        handle.close();
                    }
                };
            }
        };
    }

    @Override
    public ManagedContext requestContext() {
        return container.requestContext();
    }

    /**
     * A default fallback {@link Factory} implementation used by
     * {@link BeanContainer#beanInstanceFactory(Class, Annotation...)}.
     * <p/>
     * This factory attempts to create instances of given class by calling their no-arg constructor. Any exceptions
     * related to lack of such constructor of failure to invoke it are simply re-thrown.
     *
     * @param <T> represents the type that this factory can create
     */
    private static final class DefaultInstanceFactory<T> implements BeanContainer.Factory<T> {

        private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
        private static final MethodType VOID_TYPE = MethodType.methodType(void.class);

        private final Class<T> type;

        DefaultInstanceFactory(Class<T> type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public BeanContainer.Instance<T> create() {
            try {
                T instance = (T) LOOKUP.findConstructor(type, VOID_TYPE).invoke();
                return new BeanContainer.Instance<>() {
                    @Override
                    public T get() {
                        return instance;
                    }
                };
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        }
    }
}
