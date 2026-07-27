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

import io.quarkus.arc.ActiveResult;
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
import io.quarkus.runtime.StartupContext;
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

    public static volatile Map<String, Supplier<ActiveResult>> syntheticBeanCheckActive;

    public ArcContainer initContainer(ShutdownContext shutdown, RuntimeValue<CurrentContextFactory> currentContextFactory,
            boolean strictCompatibility, boolean testMode) throws Exception {
        ArcInitConfig.Builder builder = ArcInitConfig.builder()
                .setCurrentContextFactory(currentContextFactory != null ? currentContextFactory.getValue() : null)
                .setStrictCompatibility(strictCompatibility)
                .setTestMode(testMode);
        ArcContainer container = Arc.initialize(builder.build());
        // Arc.shutdown() is handled by the Arc lifecycle runtime service (LifecycleEventsBuildStep)
        return container;
    }

    public void initExecutor(ExecutorService executor) {
        Arc.setExecutor(executor);
    }

    public void initStaticSupplierBeans(Map<String, Function<SyntheticCreationalContext<?>, ?>> creationFunctions,
            Map<String, Supplier<ActiveResult>> checkActiveSuppliers) {
        syntheticBeanProviders = new ConcurrentHashMap<>(creationFunctions);
        syntheticBeanCheckActive = new ConcurrentHashMap<>(checkActiveSuppliers);
    }

    public void initRuntimeSupplierBeans(Map<String, Function<SyntheticCreationalContext<?>, ?>> creationFunctions,
            Map<String, Supplier<ActiveResult>> checkActiveSuppliers) {
        syntheticBeanProviders.putAll(creationFunctions);
        syntheticBeanCheckActive.putAll(checkActiveSuppliers);
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

    /**
     * @deprecated handled by the Arc lifecycle runtime service; retained for compatibility
     */
    @Deprecated(forRemoval = true)
    public void handleLifecycleEvents(ShutdownContext context, LaunchMode launchMode,
            boolean disableApplicationLifecycleObservers) {
        List<Class<?>> mockBeanClasses = computeMockBeanClasses(launchMode, disableApplicationLifecycleObservers);
        fireLifecycleEvent(new StartupEvent(), mockBeanClasses);
        context.addShutdownTask(() -> performShutdown(mockBeanClasses));
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

    /**
     * Create a creation function for a service-value-backed synthetic bean.
     * The function reads and removes the service value from the startup context
     * on first CDI access, ensuring the service action has already populated
     * the value. Removing drains the map so it does not persist after startup.
     *
     * @param serviceKey the startup context key for the service value
     * @param startupContext the startup context
     * @return a function that reads and removes the service value on demand
     */
    public Function<SyntheticCreationalContext<?>, Object> createServiceValueFunction(
            String serviceKey, StartupContext startupContext) {
        return new Function<SyntheticCreationalContext<?>, Object>() {
            @Override
            public Object apply(SyntheticCreationalContext<?> t) {
                Object value = startupContext.removeServiceValue(serviceKey);
                if (value == null) {
                    throw new IllegalStateException("Service '" + serviceKey
                            + "' has not been initialized; the CDI bean was accessed before the service action ran");
                }
                return value;
            }
        };
    }

    public void initTestApplicationClassPredicate(Set<String> applicationBeanClasses) {
        PreloadedTestApplicationClassPredicate predicate = Arc.requireContainer()
                .instance(PreloadedTestApplicationClassPredicate.class)
                .get();
        predicate.setApplicationBeanClasses(applicationBeanClasses);
    }

    /**
     * Compute the list of bean classes whose observers should be mocked in test mode.
     * Returns an empty list if mocking is not applicable.
     *
     * @param launchMode the launch mode
     * @param disableApplicationLifecycleObservers whether test observers are disabled
     * @return the list of bean classes to mock (never {@code null})
     */
    public static List<Class<?>> computeMockBeanClasses(LaunchMode launchMode, boolean disableApplicationLifecycleObservers) {
        if (launchMode == LaunchMode.TEST && disableApplicationLifecycleObservers) {
            ArcContainerImpl container = ArcContainerImpl.instance();
            Predicate<String> predicate = container
                    .select(TestApplicationClassPredicate.class).get();
            List<Class<?>> mockBeanClasses = new ArrayList<>();
            for (InjectableBean<?> bean : container.getBeans()) {
                if (bean.getKind() == Kind.CLASS && predicate.test(bean.getBeanClass().getName())) {
                    mockBeanClasses.add(bean.getBeanClass());
                }
            }
            return mockBeanClasses;
        }
        return Collections.emptyList();
    }

    /**
     * Fire a CDI lifecycle event, temporarily mocking observers for the given bean classes.
     *
     * @param event the event to fire
     * @param mockBeanClasses bean classes whose observers should be mocked (may be empty)
     */
    public static void fireLifecycleEvent(Object event, List<Class<?>> mockBeanClasses) {
        ArcContainerImpl container = ArcContainerImpl.instance();
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

    /**
     * Perform application-level shutdown: fire the {@link ShutdownEvent} and
     * clean up synthetic bean providers. Arc container destruction is handled
     * separately via the static-init service's {@code onStop} handler.
     *
     * @param mockBeanClasses bean classes whose observers should be mocked (from {@link #computeMockBeanClasses})
     */
    public static void performShutdown(List<Class<?>> mockBeanClasses) {
        fireLifecycleEvent(new ShutdownEvent(ApplicationLifecycleManager.shutdownReason), mockBeanClasses);
        syntheticBeanProviders = null;
    }

}
