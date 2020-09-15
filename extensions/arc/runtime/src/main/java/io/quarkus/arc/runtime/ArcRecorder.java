package io.quarkus.arc.runtime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ArcRecorder {

    /**
     * Used to hold the Supplier instances used for synthetic bean declarations.
     */
    public static volatile Map<String, Supplier<?>> supplierMap;

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

    public void initStaticSupplierBeans(Map<String, Supplier<?>> beans) {
        supplierMap = new ConcurrentHashMap<>(beans);
    }

    public void initRuntimeSupplierBeans(Map<String, Supplier<?>> beans) {
        supplierMap.putAll(beans);
    }

    public BeanContainer initBeanContainer(ArcContainer container, List<BeanContainerListener> listeners)
            throws Exception {
        if (container == null) {
            throw new IllegalArgumentException("Arc container was null");
        }
        BeanContainer beanContainer = new BeanContainerImpl(container);
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

}
