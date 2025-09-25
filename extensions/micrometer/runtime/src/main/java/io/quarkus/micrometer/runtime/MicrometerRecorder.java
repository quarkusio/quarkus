package io.quarkus.micrometer.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

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
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.quarkus.arc.Arc;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.binder.JVMInfoBinder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.runtime.config.runtime.HttpClientConfig;
import io.quarkus.micrometer.runtime.config.runtime.HttpServerConfig;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;
import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RuntimeInit;
import io.quarkus.runtime.annotations.StaticInit;
import io.quarkus.runtime.metrics.MetricsFactory;

@Recorder
public class MicrometerRecorder {
    private static final Logger log = Logger.getLogger(MicrometerRecorder.class);
    static final String DEFAULT_EXCEPTION_TAG_VALUE = "none";
    static MicrometerMetricsFactory factory;
    public static String nonApplicationUri = "/q/";
    public static String httpRootUri = "/";

    private final MicrometerConfig config;
    private final RuntimeValue<HttpServerConfig> httpServerConfig;
    private final RuntimeValue<HttpClientConfig> httpClientConfig;
    private final RuntimeValue<VertxConfig> vertxConfig;

    public MicrometerRecorder(
            final MicrometerConfig config,
            final RuntimeValue<HttpServerConfig> httpServerConfig,
            final RuntimeValue<HttpClientConfig> clientConfig,
            final RuntimeValue<VertxConfig> vertxConfig) {
        this.config = config;
        this.httpServerConfig = httpServerConfig;
        this.httpClientConfig = clientConfig;
        this.vertxConfig = vertxConfig;
    }

    @StaticInit
    public RuntimeValue<MeterRegistry> createRootRegistry(String qUri, String httpUri) {
        CompositeMeterRegistry globalRegistry = Metrics.globalRegistry;
        factory = new MicrometerMetricsFactory(config, globalRegistry);
        nonApplicationUri = qUri;
        httpRootUri = httpUri;
        return new RuntimeValue<>(globalRegistry);
    }

    @RuntimeInit
    public void configureRegistries(Set<Class<? extends MeterRegistry>> registryClasses, ShutdownContext context) {
        BeanManager beanManager = Arc.container().beanManager();

        Map<Class<? extends MeterRegistry>, List<MeterFilter>> classMeterFilters = new HashMap<>(registryClasses.size());
        List<MeterFilter> globalFilters = new ArrayList<>();
        populateMeterFilters(registryClasses, beanManager, classMeterFilters, globalFilters);

        Map<Class<? extends MeterRegistry>, List<MeterRegistryCustomizer>> classMeterRegistryCustomizers = new HashMap<>(
                registryClasses.size());
        List<MeterRegistryCustomizer> globalMeterRegistryCustomizers = new ArrayList<>();
        populateMeterRegistryCustomizers(registryClasses, beanManager, classMeterRegistryCustomizers,
                globalMeterRegistryCustomizers);

        // Find and configure MeterRegistry beans (includes runtime config)
        Set<Bean<?>> beans = new HashSet<>(beanManager.getBeans(MeterRegistry.class, Any.Literal.INSTANCE));
        beans.removeIf(bean -> bean.getBeanClass().equals(CompositeRegistryCreator.class));

        // Apply global filters to the global registry
        applyMeterFilters(Metrics.globalRegistry, globalFilters);
        applyMeterRegistryCustomizers(Metrics.globalRegistry, globalMeterRegistryCustomizers);

        for (Bean<?> i : beans) {
            MeterRegistry registry = (MeterRegistry) beanManager
                    .getReference(i, MeterRegistry.class, beanManager.createCreationalContext(i));

            // Add & configure non-root registries
            if (registry != Metrics.globalRegistry && registry != null) {
                applyMeterFilters(registry, globalFilters);
                Class<?> registryClass = registry.getClass();
                applyMeterFilters(registry, classMeterFilters.get(registryClass));

                var classSpecificCustomizers = classMeterRegistryCustomizers.getOrDefault(registryClass,
                        Collections.emptyList());
                var newList = new ArrayList<MeterRegistryCustomizer>(
                        globalMeterRegistryCustomizers.size() + classSpecificCustomizers.size());
                newList.addAll(globalMeterRegistryCustomizers);
                newList.addAll(classSpecificCustomizers);
                applyMeterRegistryCustomizers(registry, newList);

                log.debugf("Adding configured registry %s", registryClass, registry);
                Metrics.globalRegistry.add(registry);
            }
        }

        List<AutoCloseable> autoCloseables = new ArrayList<>();

        // Base JVM Metrics
        if (config.checkBinderEnabledWithDefault(() -> config.binder().jvm())) {
            new ClassLoaderMetrics().bindTo(Metrics.globalRegistry);
            JvmHeapPressureMetrics jvmHeapPressureMetrics = new JvmHeapPressureMetrics();
            jvmHeapPressureMetrics.bindTo(Metrics.globalRegistry);
            autoCloseables.add(jvmHeapPressureMetrics);
            new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
            new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
            new JVMInfoBinder().bindTo(Metrics.globalRegistry);
            if (ImageMode.current() == ImageMode.JVM) {
                JvmGcMetrics jvmGcMetrics = new JvmGcMetrics();
                jvmGcMetrics.bindTo(Metrics.globalRegistry);
                autoCloseables.add(jvmGcMetrics);
            }
        }

        // System metrics
        if (config.checkBinderEnabledWithDefault(() -> config.binder().system())) {
            new UptimeMetrics().bindTo(Metrics.globalRegistry);
            new ProcessorMetrics().bindTo(Metrics.globalRegistry);
            new FileDescriptorMetrics().bindTo(Metrics.globalRegistry);
        }

        // Discover and bind MeterBinders (includes annotated gauges, etc.)
        // This must be done at runtime. If done before backend registries are
        // configured, some measurements may be missed.
        Instance<MeterBinder> allBinders = beanManager.createInstance()
                .select(MeterBinder.class, Any.Literal.INSTANCE);
        for (MeterBinder meterBinder : allBinders) {
            meterBinder.bindTo(Metrics.globalRegistry);
        }

        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
                    // Drop existing meters (recreated on next use)
                    for (Meter meter : Metrics.globalRegistry.getMeters()) {
                        Metrics.globalRegistry.remove(meter);
                    }
                }
                // iterate over defensive copy to avoid ConcurrentModificationException
                for (MeterRegistry meterRegistry : new ArrayList<>(Metrics.globalRegistry.getRegistries())) {
                    meterRegistry.close();
                    Metrics.removeRegistry(meterRegistry);
                }
                // iterate over the auto-closeables and close them
                for (AutoCloseable autoCloseable : autoCloseables) {
                    try {
                        autoCloseable.close();
                    } catch (Exception e) {
                        log.error("Error closing", e);
                    }
                }
            }
        });
    }

    private void populateMeterFilters(Set<Class<? extends MeterRegistry>> registryClasses, BeanManager beanManager,
            Map<Class<? extends MeterRegistry>, List<MeterFilter>> classMeterFilters,
            List<MeterFilter> globalFilters) {
        // Find global/common registry configuration
        Instance<MeterFilter> globalFilterInstance = beanManager.createInstance()
                .select(MeterFilter.class, Default.Literal.INSTANCE);
        populateList(globalFilterInstance, globalFilters);
        log.debugf("Discovered global MeterFilters : %s", globalFilters);

        // Find MeterFilters for specific registry classes, i.e.:
        // @MeterFilterConstraint(applyTo = DatadogMeterRegistry.class) Instance<MeterFilter> filters
        for (Class<? extends MeterRegistry> typeClass : registryClasses) {
            Instance<MeterFilter> classFilterInstance = beanManager.createInstance()
                    .select(MeterFilter.class, new MeterFilterConstraint.Literal(typeClass));
            List<MeterFilter> classFilters = classMeterFilters.computeIfAbsent(typeClass, k -> new ArrayList<>());

            populateList(classFilterInstance, classFilters);
            log.debugf("Discovered MeterFilters for %s: %s", typeClass, classFilters);
        }
    }

    private void populateMeterRegistryCustomizers(Set<Class<? extends MeterRegistry>> registryClasses, BeanManager beanManager,
            Map<Class<? extends MeterRegistry>, List<MeterRegistryCustomizer>> classMeterRegistryCustomizers,
            List<MeterRegistryCustomizer> globalMeterRegistryCustomizers) {
        // Find global/common registry configuration
        Instance<MeterRegistryCustomizer> globalFilterInstance = beanManager.createInstance()
                .select(MeterRegistryCustomizer.class, Default.Literal.INSTANCE);
        populateList(globalFilterInstance, globalMeterRegistryCustomizers);
        log.debugf("Discovered global MeterRegistryCustomizer : %s", globalMeterRegistryCustomizers);

        // Find MeterRegistryCustomizers for specific registry classes, i.e.:
        // @MeterRegistryCustomizerConstraint(applyTo = DatadogMeterRegistryCustomizer.class) Instance<MeterRegistryCustomizer> customizers
        log.debugf("Configuring Micrometer registries : %s", registryClasses);
        for (Class<? extends MeterRegistry> typeClass : registryClasses) {
            Instance<MeterRegistryCustomizer> classFilterInstance = beanManager.createInstance()
                    .select(MeterRegistryCustomizer.class, new MeterRegistryCustomizerConstraint.Literal(typeClass));
            List<MeterRegistryCustomizer> classFilters = classMeterRegistryCustomizers.computeIfAbsent(typeClass,
                    k -> new ArrayList<>());

            populateList(classFilterInstance, classFilters);
            log.debugf("Discovered MeterRegistryCustomizer for %s: %s", typeClass, classFilters);
        }
    }

    private <T> void populateList(Instance<T> filterInstance, List<T> meterFilters) {
        if (!filterInstance.isUnsatisfied()) {
            for (T filter : filterInstance) {
                // @Produces methods can return null, and those will turn up here.
                if (filter != null) {
                    meterFilters.add(filter);
                }
            }
        }
    }

    private void applyMeterFilters(MeterRegistry registry, List<MeterFilter> filters) {
        if (filters != null) {
            for (MeterFilter meterFilter : filters) {
                registry.config().meterFilter(meterFilter);
            }
        }
    }

    private void applyMeterRegistryCustomizers(MeterRegistry registry, List<MeterRegistryCustomizer> customizers) {
        if ((customizers != null) && !customizers.isEmpty()) {
            Collections.sort(customizers);
            for (MeterRegistryCustomizer customizer : customizers) {
                customizer.customize(registry);
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
        } catch (ClassNotFoundException ignored) {
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

    @RuntimeInit
    public RuntimeValue<HttpBinderConfiguration> configureHttpMetrics(boolean httpServerMetricsEnabled,
            boolean httpClientMetricsEnabled) {
        return new RuntimeValue<HttpBinderConfiguration>(
                new HttpBinderConfiguration(httpServerMetricsEnabled, httpClientMetricsEnabled, httpServerConfig.getValue(),
                        httpClientConfig.getValue(), vertxConfig.getValue()));
    }
}
