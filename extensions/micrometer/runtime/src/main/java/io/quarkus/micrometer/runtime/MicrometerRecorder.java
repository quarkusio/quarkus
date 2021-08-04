package io.quarkus.micrometer.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.quarkus.arc.Arc;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.binder.JVMInfoBinder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.runtime.config.runtime.HttpClientConfig;
import io.quarkus.micrometer.runtime.config.runtime.HttpServerConfig;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.metrics.MetricsFactory;

@Recorder
public class MicrometerRecorder {
    private static final Logger log = Logger.getLogger(MicrometerRecorder.class);
    static final String DEFAULT_EXCEPTION_TAG_VALUE = "none";
    static MicrometerMetricsFactory factory;

    /* STATIC_INIT */
    public RuntimeValue<MeterRegistry> createRootRegistry(MicrometerConfig config) {
        factory = new MicrometerMetricsFactory(config, Metrics.globalRegistry);
        return new RuntimeValue<>(Metrics.globalRegistry);
    }

    /* RUNTIME_INIT */
    public void configureRegistries(MicrometerConfig config,
            Set<Class<? extends MeterRegistry>> registryClasses,
            ShutdownContext context) {
        BeanManager beanManager = Arc.container().beanManager();

        Map<Class<? extends MeterRegistry>, List<MeterFilter>> classMeterFilters = new HashMap<>(registryClasses.size());

        // Find global/common registry configuration
        List<MeterFilter> globalFilters = new ArrayList<>();
        Instance<MeterFilter> globalFilterInstance = beanManager.createInstance()
                .select(MeterFilter.class, Default.Literal.INSTANCE);
        populateListWithMeterFilters(globalFilterInstance, globalFilters);
        log.debugf("Discovered global MeterFilters : %s", globalFilters);

        // Find MeterFilters for specific registry classes, i.e.:
        // @MeterFilterConstraint(applyTo = DatadogMeterRegistry.class) Instance<MeterFilter> filters
        log.debugf("Configuring Micrometer registries : %s", registryClasses);
        for (Class<? extends MeterRegistry> typeClass : registryClasses) {
            Instance<MeterFilter> classFilterInstance = beanManager.createInstance()
                    .select(MeterFilter.class, new MeterFilterConstraint.Literal(typeClass));
            List<MeterFilter> classFilters = classMeterFilters.computeIfAbsent(typeClass, k -> new ArrayList<>());

            populateListWithMeterFilters(classFilterInstance, classFilters);
            log.debugf("Discovered MeterFilters for %s: %s", typeClass, classFilters);
        }

        // Find and configure MeterRegistry beans (includes runtime config)
        Set<Bean<?>> beans = new HashSet<>(beanManager.getBeans(MeterRegistry.class, Any.Literal.INSTANCE));
        beans.removeIf(bean -> bean.getBeanClass().equals(CompositeRegistryCreator.class));

        // Apply global filters to the global registry
        applyMeterFilters(Metrics.globalRegistry, globalFilters);

        for (Bean<?> i : beans) {
            MeterRegistry registry = (MeterRegistry) beanManager
                    .getReference(i, MeterRegistry.class, beanManager.createCreationalContext(i));

            // Add & configure non-root registries
            if (registry != Metrics.globalRegistry && registry != null) {
                applyMeterFilters(registry, globalFilters);
                applyMeterFilters(registry, classMeterFilters.get(registry.getClass()));
                log.debugf("Adding configured registry %s", registry.getClass(), registry);
                Metrics.globalRegistry.add(registry);
            }
        }

        // Base JVM Metrics
        if (config.checkBinderEnabledWithDefault(() -> config.binder.jvm)) {
            new ClassLoaderMetrics().bindTo(Metrics.globalRegistry);
            new JvmHeapPressureMetrics().bindTo(Metrics.globalRegistry);
            new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
            new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
            new JVMInfoBinder().bindTo(Metrics.globalRegistry);
            if (!ImageInfo.inImageCode()) {
                new JvmGcMetrics().bindTo(Metrics.globalRegistry);
            }
        }

        // System metrics
        if (config.checkBinderEnabledWithDefault(() -> config.binder.system)) {
            new UptimeMetrics().bindTo(Metrics.globalRegistry);
            new ProcessorMetrics().bindTo(Metrics.globalRegistry);
            new FileDescriptorMetrics().bindTo(Metrics.globalRegistry);
        }

        // Discover and bind MeterBinders (includes annotated gauges, etc)
        // This must be done at runtime. If done before backend registries are
        // configured, some measurements may be missed.
        Instance<MeterBinder> allBinders = beanManager.createInstance()
                .select(MeterBinder.class, Any.Literal.INSTANCE);
        allBinders.forEach(x -> x.bindTo(Metrics.globalRegistry));

        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
                    // Drop existing meters (recreated on next use)
                    Collection<Meter> meters = new ArrayList<>(Metrics.globalRegistry.getMeters());
                    meters.forEach(m -> Metrics.globalRegistry.remove(m));
                }
                Collection<MeterRegistry> cleanup = new ArrayList<>(Metrics.globalRegistry.getRegistries());
                cleanup.forEach(x -> {
                    x.close();
                    Metrics.removeRegistry(x);
                });
            }
        });
    }

    void populateListWithMeterFilters(Instance<MeterFilter> filterInstance, List<MeterFilter> meterFilters) {
        if (!filterInstance.isUnsatisfied()) {
            for (MeterFilter filter : filterInstance) {
                // @Produces methods can return null, and those will turn up here.
                if (filter != null) {
                    meterFilters.add(filter);
                }
            }
        }
    }

    void applyMeterFilters(MeterRegistry registry, List<MeterFilter> filters) {
        if (filters != null) {
            for (MeterFilter meterFilter : filters) {
                registry.config().meterFilter(meterFilter);
            }
        }
    }

    public void registerMetrics(Consumer<MetricsFactory> consumer) {
        consumer.accept(factory);
    }

    public static Class<?> getClassForName(String classname) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(classname, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
        }
        log.debugf("getClass: TCCL: %s ## %s : %s", Thread.currentThread().getContextClassLoader(), classname, (clazz != null));
        return clazz;
    }

    static String getExceptionTag(Throwable throwable) {
        if (throwable == null) {
            return DEFAULT_EXCEPTION_TAG_VALUE;
        }
        if (throwable.getCause() == null) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getCause().getClass().getSimpleName();
    }

    /* RUNTIME_INIT */
    public RuntimeValue<HttpBinderConfiguration> configureHttpMetrics(
            boolean httpServerMetricsEnabled,
            boolean httpClientMetricsEnabled,
            HttpServerConfig serverConfig,
            HttpClientConfig clientConfig,
            VertxConfig vertxConfig) {
        return new RuntimeValue<HttpBinderConfiguration>(
                new HttpBinderConfiguration(httpServerMetricsEnabled,
                        httpClientMetricsEnabled,
                        serverConfig, clientConfig, vertxConfig));
    }
}
