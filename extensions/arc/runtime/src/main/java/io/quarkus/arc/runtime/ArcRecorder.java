package io.quarkus.arc.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ArcInitConfig;
import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableBean.Kind;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.impl.ArcContainerImpl;
import io.quarkus.arc.runtime.test.PreloadedTestApplicationClassPredicate;
import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.test.TestApplicationClassPredicate;

@Recorder
public class ArcRecorder {

    private static final Logger LOG = Logger.getLogger(ArcRecorder.class);

    /**
     * Used to hold the Supplier instances used for synthetic bean declarations.
     */
    public static volatile Map<String, Function<SyntheticCreationalContext<?>, ?>> syntheticBeanProviders;

    public ArcContainer initContainer(ShutdownContext shutdown, RuntimeValue<CurrentContextFactory> currentContextFactory,
            boolean strictCompatibility) throws Exception {
        ArcInitConfig.Builder builder = ArcInitConfig.builder();
        builder.setCurrentContextFactory(currentContextFactory != null ? currentContextFactory.getValue() : null);
        builder.setStrictCompatibility(strictCompatibility);
        ArcContainer container = Arc.initialize(builder.build());
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

    public void initStaticSupplierBeans(Map<String, Function<SyntheticCreationalContext<?>, ?>> beans) {
        syntheticBeanProviders = new ConcurrentHashMap<>(beans);
    }

    public void initRuntimeSupplierBeans(Map<String, Function<SyntheticCreationalContext<?>, ?>> beans) {
        syntheticBeanProviders.putAll(beans);
    }

    public BeanContainer initBeanContainer(ArcContainer container, List<BeanContainerListener> listeners)
            throws Exception {
        if (container == null) {
            throw new IllegalArgumentException("Arc container was null");
        }
        BeanContainer beanContainer = new BeanContainerImpl(container);
        for (BeanContainerListener listener : listeners) {
            long start = System.currentTimeMillis();
            listener.created(beanContainer);
            LOG.debugf("Bean container listener %s finished in %s ms", listener.getClass().getName(),
                    System.currentTimeMillis() - start);
        }
        return beanContainer;
    }

    public void handleLifecycleEvents(ShutdownContext context, LaunchMode launchMode,
            boolean disableApplicationLifecycleObservers) {
        ArcContainerImpl container = ArcContainerImpl.instance();
        List<Class<?>> mockBeanClasses;

        // If needed then mock all app observers in the test mode
        if (launchMode == LaunchMode.TEST && disableApplicationLifecycleObservers) {
            Predicate<String> predicate = container
                    .select(TestApplicationClassPredicate.class).get();
            mockBeanClasses = new ArrayList<>();
            for (InjectableBean<?> bean : container.getBeans()) {
                // Mock observers for all application class beans
                if (bean.getKind() == Kind.CLASS && predicate.test(bean.getBeanClass().getName())) {
                    mockBeanClasses.add(bean.getBeanClass());
                }
            }
        } else {
            mockBeanClasses = Collections.emptyList();
        }

        fireLifecycleEvent(container, new StartupEvent(), mockBeanClasses);

        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                fireLifecycleEvent(container, new ShutdownEvent(ApplicationLifecycleManager.shutdownReason), mockBeanClasses);
            }
        });
    }

    public Function<SyntheticCreationalContext<?>, Object> createFunction(RuntimeValue<?> value) {
        return new Function<SyntheticCreationalContext<?>, Object>() {
            @Override
            public Object apply(SyntheticCreationalContext<?> t) {
                return value.getValue();
            }
        };
    }

    public Function<SyntheticCreationalContext<?>, Object> createFunction(Supplier<?> supplier) {
        return new Function<SyntheticCreationalContext<?>, Object>() {
            @Override
            public Object apply(SyntheticCreationalContext<?> t) {
                return supplier.get();
            }
        };
    }

    public Function<SyntheticCreationalContext<?>, Object> createFunction(Object returnedProxy) {
        return new Function<SyntheticCreationalContext<?>, Object>() {
            @Override
            public Object apply(SyntheticCreationalContext<?> t) {
                return returnedProxy;
            }
        };
    }

    public void initTestApplicationClassPredicate(Set<String> applicationBeanClasses) {
        PreloadedTestApplicationClassPredicate predicate = Arc.container()
                .instance(PreloadedTestApplicationClassPredicate.class).get();
        predicate.setApplicationBeanClasses(applicationBeanClasses);
    }

    private void fireLifecycleEvent(ArcContainerImpl container, Object event, List<Class<?>> mockBeanClasses) {
        if (!mockBeanClasses.isEmpty()) {
            for (Class<?> beanClass : mockBeanClasses) {
                container.mockObserversFor(beanClass, true);
            }
        }
        container.beanManager().getEvent().fire(event);
        if (!mockBeanClasses.isEmpty()) {
            for (Class<?> beanClass : mockBeanClasses) {
                container.mockObserversFor(beanClass, false);
            }
        }
    }

}
