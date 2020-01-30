package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

/**
 */
@Recorder
public class ArcRecorder {

    /**
     * Used to hold the Supplier instances used for synthetic bean declarations.
     */
    public static volatile Map<String, Supplier<?>> supplierMap;

    private static final Logger LOGGER = Logger.getLogger(ArcRecorder.class.getName());

    public ArcContainer getContainer(ShutdownContext shutdown) throws Exception {
        ArcContainer container = Arc.initialize();
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                Arc.shutdown();
            }
        });
        return container;
    }

    public void initExecutor(ExecutorService executor) {
        Arc.setExecutor(executor);
    }

    public void initSupplierBeans(Map<String, Supplier<?>> beans) {
        supplierMap = new ConcurrentHashMap<>(beans);
    }

    public BeanContainer initBeanContainer(ArcContainer container, List<BeanContainerListener> listeners,
            Collection<String> removedBeanTypes)
            throws Exception {

        if (container == null) {
            throw new IllegalArgumentException("Arc container was null");
        }
        BeanContainer beanContainer = new BeanContainer() {
            @Override
            public <T> Factory<T> instanceFactory(Class<T> type, Annotation... qualifiers) {
                Supplier<InstanceHandle<T>> handleSupplier = container.instanceSupplier(type, qualifiers);
                if (handleSupplier == null) {
                    if (removedBeanTypes.contains(type.getName())) {
                        // Note that this only catches the simplest use cases
                        LOGGER.warnf(
                                "Bean matching %s was marked as unused and removed during build.\nExtensions can eliminate false positives using:\n\t- a custom UnremovableBeanBuildItem\n\t- AdditionalBeanBuildItem(false, beanClazz)",
                                type);
                    } else {
                        LOGGER.debugf(
                                "No matching bean found for type %s and qualifiers %s. The bean might have been marked as unused and removed during build.",
                                type, Arrays.toString(qualifiers));
                    }
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
        };
        for (BeanContainerListener listener : listeners) {
            listener.created(beanContainer);
        }
        return beanContainer;
    }

    public void handleLifecycleEvents(ShutdownContext context, BeanContainer beanContainer) {
        LifecycleEventRunner instance = beanContainer.instance(LifecycleEventRunner.class);
        instance.fireStartupEvent();
        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                instance.fireShutdownEvent();
            }
        });
    }

    public Supplier<Object> createSupplier(RuntimeValue<?> value) {
        return new Supplier<Object>() {
            @Override
            public Object get() {
                return value.getValue();
            }
        };
    }

    private static final class DefaultInstanceFactory<T> implements BeanContainer.Factory<T> {

        final Class<T> type;

        private DefaultInstanceFactory(Class<T> type) {
            this.type = type;
        }

        @Override
        public BeanContainer.Instance<T> create() {
            try {
                T instance = type.newInstance();
                return new BeanContainer.Instance<T>() {
                    @Override
                    public T get() {
                        return instance;
                    }
                };
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
