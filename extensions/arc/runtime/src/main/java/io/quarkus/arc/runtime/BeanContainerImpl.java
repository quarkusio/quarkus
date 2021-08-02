package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;

public class BeanContainerImpl implements BeanContainer {

    private static final Logger LOGGER = Logger.getLogger(BeanContainerImpl.class.getName());

    private final ArcContainer container;

    public BeanContainerImpl(ArcContainer container) {
        this.container = container;
    }

    @Override
    public <T> Factory<T> instanceFactory(Class<T> type, Annotation... qualifiers) {
        Supplier<InstanceHandle<T>> handleSupplier = container.instanceSupplier(type, qualifiers);
        if (handleSupplier == null) {
            LOGGER.debugf(
                    "No matching bean found for type %s and qualifiers %s. The bean might have been marked as unused and removed during build.",
                    type, Arrays.toString(qualifiers));
            return new DefaultInstanceFactory<>(type);
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

    static final class DefaultInstanceFactory<T> implements BeanContainer.Factory<T> {

        final Class<T> type;

        DefaultInstanceFactory(Class<T> type) {
            this.type = type;
        }

        @Override
        public BeanContainer.Instance<T> create() {
            try {
                T instance = type.getDeclaredConstructor().newInstance();
                return new BeanContainer.Instance<T>() {
                    @Override
                    public T get() {
                        return instance;
                    }
                };
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
