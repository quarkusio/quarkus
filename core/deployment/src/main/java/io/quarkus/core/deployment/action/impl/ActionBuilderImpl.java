package io.quarkus.core.deployment.action.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.core.deployment.action.ServiceBuilder0;
import io.quarkus.core.deployment.action.VoidServiceBuilder0;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.proxy.ProxyConfiguration;
import io.quarkus.deployment.proxy.ProxyFactory;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.RuntimeValue;

/**
 * The entry point for defining services from build steps.
 * <p>
 * An {@code ActionBuilder} is injected as a parameter into {@code @BuildStep} methods.
 * It provides a fluent API for defining services that execute at application startup:
 *
 * <pre>{@code
 * @BuildStep
 * void setupMyService(ActionBuilder action) {
 *     action.forService(MyService.class)
 *             .require(SomeDependency.class)
 *             .action((ctx, dep) -> new MyService(dep));
 * }
 * }</pre>
 * <p>
 * Each call to {@code action()} or {@code actionAsync()} on the terminal builder
 * extracts the lambda into a {@link TransliteratedAction} record for deferred
 * consolidated class generation.
 */
public final class ActionBuilderImpl implements ActionBuilder {

    private final Consumer<MainBytecodeRecorderBuildItem> recorderProducer;
    private final Consumer<StaticBytecodeRecorderBuildItem> staticRecorderProducer;
    private final Consumer<ServiceMetadataBuildItem> metadataProducer;
    private final Consumer<StaticServiceMetadataBuildItem> staticMetadataProducer;
    private final Consumer<ServiceValueRetentionBuildItem> retentionProducer;
    private final String buildStepName;

    /**
     * Construct a new instance.
     * This constructor is called by the extension loader infrastructure.
     *
     * @param recorderProducer consumer for producing {@link MainBytecodeRecorderBuildItem}s
     * @param staticRecorderProducer consumer for producing {@link StaticBytecodeRecorderBuildItem}s
     * @param metadataProducer consumer for producing {@link ServiceMetadataBuildItem}s
     * @param staticMetadataProducer consumer for producing {@link StaticServiceMetadataBuildItem}s
     * @param retentionProducer consumer for producing {@link ServiceValueRetentionBuildItem}s
     * @param buildStepName the name of the declaring build step (for diagnostics)
     */
    public ActionBuilderImpl(
            Consumer<MainBytecodeRecorderBuildItem> recorderProducer,
            Consumer<StaticBytecodeRecorderBuildItem> staticRecorderProducer,
            Consumer<ServiceMetadataBuildItem> metadataProducer,
            Consumer<StaticServiceMetadataBuildItem> staticMetadataProducer,
            Consumer<ServiceValueRetentionBuildItem> retentionProducer,
            String buildStepName) {
        this.recorderProducer = recorderProducer;
        this.staticRecorderProducer = staticRecorderProducer;
        this.metadataProducer = metadataProducer;
        this.staticMetadataProducer = staticMetadataProducer;
        this.retentionProducer = retentionProducer;
        this.buildStepName = buildStepName;
    }

    public <T> ServiceBuilder0<T> forService(Class<T> serviceType) {
        return forService(serviceType, List.of());
    }

    public <T> ServiceBuilder0<T> forService(Class<T> serviceType, String serviceName) {
        return forService(serviceType, serviceName == null ? List.of() : List.of(serviceName));
    }

    public <T> ServiceBuilder0<T> forService(Class<T> serviceType, List<String> serviceNameParts) {
        return new ServiceBuilderImpl0<>(new ServiceBuilderImpl<>(
                serviceType,
                serviceNameParts,
                recorderProducer,
                staticRecorderProducer,
                metadataProducer,
                staticMetadataProducer,
                buildStepName));
    }

    public VoidServiceBuilder0 forService(String serviceName) {
        return forVoidService(List.of(serviceName));
    }

    public VoidServiceBuilder0 forService(List<String> serviceNameParts) {
        return forVoidService(serviceNameParts);
    }

    private VoidServiceBuilder0 forVoidService(List<String> serviceNameParts) {
        return new VoidServiceBuilderImpl0(new ServiceBuilderImpl<>(
                Void.class,
                serviceNameParts,
                recorderProducer,
                staticRecorderProducer,
                metadataProducer,
                staticMetadataProducer,
                buildStepName));
    }

    public <T> void aliasRecorderValue(Class<T> serviceType, T recorderValue) {
        aliasRecorderValue(serviceType, null, recorderValue, Phase.APPLICATION);
    }

    public <T> void aliasRecorderValue(Class<T> serviceType, T recorderValue, Phase phase) {
        aliasRecorderValue(serviceType, null, recorderValue, phase);
    }

    public <T> void aliasRecorderValue(Class<T> serviceType, String serviceName, T recorderValue) {
        aliasRecorderValue(serviceType, serviceName, recorderValue, Phase.APPLICATION);
    }

    public <T> void aliasRecorderValue(Class<T> serviceType, String serviceName, T recorderValue, Phase phase) {
        if (!(recorderValue instanceof BytecodeRecorderImpl.ReturnedProxy rp)) {
            throw new IllegalArgumentException(
                    "aliasRecorderValue requires a recorder proxy (an object returned from a @Recorder method), "
                            + "but got: " + recorderValue.getClass().getName());
        }
        String recorderProxyKey = rp.__returned$proxy$key();
        boolean staticInit = rp.__static$$init();
        List<String> serviceNameParts = serviceName == null ? List.of() : List.of(serviceName);
        String serviceKey = LambdaTransliterator.serviceKey(serviceType, serviceNameParts);

        // produce a TransliteratedAction.AliasService instead of generating a class
        TransliteratedAction alias = new TransliteratedAction.AliasService(serviceKey, staticInit, recorderProxyKey,
                buildStepName);

        // produce the appropriate phase-specific build items
        if (staticInit) {
            staticRecorderProducer.accept(new StaticBytecodeRecorderBuildItem(alias));
            staticMetadataProducer.accept(new StaticServiceMetadataBuildItem(
                    serviceType, serviceNameParts, List.of(), buildStepName));
        } else {
            recorderProducer.accept(new MainBytecodeRecorderBuildItem(alias));
            metadataProducer.accept(new ServiceMetadataBuildItem(
                    serviceType, serviceNameParts, List.of(), buildStepName, phase));
        }
    }

    // ── Service → RuntimeValue bridge ──

    public <T> RuntimeValue<T> serviceAsRuntimeValue(Class<T> serviceType) {
        return serviceAsRuntimeValue(serviceType, List.of());
    }

    public <T> RuntimeValue<T> serviceAsRuntimeValue(Class<T> serviceType, String serviceName) {
        return serviceAsRuntimeValue(serviceType, serviceName == null ? List.of() : List.of(serviceName));
    }

    public <T> RuntimeValue<T> serviceAsRuntimeValue(Class<T> serviceType, List<String> serviceNameParts) {
        return createRuntimeValueProxy(serviceType, serviceNameParts, false);
    }

    public <T> RuntimeValue<T> staticInitServiceAsRuntimeValue(Class<T> serviceType) {
        return staticInitServiceAsRuntimeValue(serviceType, List.of());
    }

    public <T> RuntimeValue<T> staticInitServiceAsRuntimeValue(Class<T> serviceType, String serviceName) {
        return staticInitServiceAsRuntimeValue(serviceType, serviceName == null ? List.of() : List.of(serviceName));
    }

    public <T> RuntimeValue<T> staticInitServiceAsRuntimeValue(Class<T> serviceType, List<String> serviceNameParts) {
        return createRuntimeValueProxy(serviceType, serviceNameParts, true);
    }

    /**
     * Create a {@link RuntimeValue} proxy that implements {@link BytecodeRecorderImpl.ReturnedProxy}
     * and register a {@link TransliteratedAction.RuntimeValueWrapper} to wrap the service value at runtime.
     *
     * @param serviceType the service type
     * @param serviceNameParts the service name parts (must not be {@code null})
     * @param staticInit whether this targets the static-init phase
     * @param <T> the service type
     * @return a {@code RuntimeValue<T>} proxy
     */
    private <T> RuntimeValue<T> createRuntimeValueProxy(Class<T> serviceType, List<String> serviceNameParts,
            boolean staticInit) {
        // the RuntimeValueWrapper copies the raw service value into a RuntimeValue and stores it
        // in the serviceValues map; this is needed because recorder methods that receive
        // RuntimeValue<T> expect to call getValue() on a RuntimeValue, not receive the raw value
        String serviceKey = LambdaTransliterator.serviceKey(serviceType, serviceNameParts);
        String rvKey = "runtimevalue:" + serviceKey;

        TransliteratedAction wrapper = new TransliteratedAction.RuntimeValueWrapper(serviceKey, staticInit, rvKey,
                buildStepName);
        if (staticInit) {
            staticRecorderProducer.accept(new StaticBytecodeRecorderBuildItem(wrapper));
            // the rvKey is consumed by a runtime-init recorder proxy; retain across phases
            trackCrossPhaseKey(rvKey);
        } else {
            recorderProducer.accept(new MainBytecodeRecorderBuildItem(wrapper));
        }

        return new ServiceRuntimeValueProxy<>(rvKey, staticInit);
    }

    // ── Service → bare recorder proxy bridge ──

    public <T> T serviceAsRecorderValue(Class<T> serviceType) {
        return serviceAsRecorderValue(serviceType, null);
    }

    public <T> T serviceAsRecorderValue(Class<T> serviceType, String serviceName) {
        return createServiceProxy(serviceType, serviceName, false);
    }

    public <T> T staticInitServiceAsRecorderValue(Class<T> serviceType) {
        return staticInitServiceAsRecorderValue(serviceType, null);
    }

    public <T> T staticInitServiceAsRecorderValue(Class<T> serviceType, String serviceName) {
        return createServiceProxy(serviceType, serviceName, true);
    }

    // ── Service → Supplier bridge (temporary) ──

    @SuppressWarnings("unchecked")
    public <T> Supplier<T> serviceAsRecorderSupplier(Class<T> serviceType) {
        return serviceAsRecorderSupplier(serviceType, null);
    }

    @SuppressWarnings("unchecked")
    public <T> Supplier<T> serviceAsRecorderSupplier(Class<T> serviceType, String serviceName) {
        return (Supplier<T>) createSupplierProxy(serviceType, serviceName, false);
    }

    /**
     * Create a bare {@code T} proxy that implements {@link BytecodeRecorderImpl.ReturnedProxy},
     * using {@link ProxyFactory} to generate a class extending/implementing the service type.
     * No deploy action is needed — the service already stores {@code T} at its service key.
     * <p>
     * Requires the deployment classloader (with {@code visibleDefineClass}) on the
     * current thread's context classloader.
     *
     * @param serviceType the service type
     * @param serviceName the optional service name
     * @param staticInit whether this targets the static-init phase
     * @param <T> the service type
     * @return a proxy implementing both the service type and {@code ReturnedProxy}
     */
    private <T> T createServiceProxy(Class<T> serviceType, String serviceName, boolean staticInit) {
        List<String> serviceNameParts = serviceName == null ? List.of() : List.of(serviceName);
        String serviceKey = LambdaTransliterator.serviceKey(serviceType, serviceNameParts);

        if (staticInit) {
            // this key is consumed by a runtime-init recorder proxy; retain across phases
            trackCrossPhaseKey(serviceKey);
        }

        ProxyConfiguration<T> config = new ProxyConfiguration<T>()
                .setAnchorClass(ActionBuilderImpl.class)
                .setClassLoader(Thread.currentThread().getContextClassLoader())
                .setSuperClass(serviceType.isInterface() ? Object.class : serviceType)
                .setProxyNameSuffix("$$ServiceRecorderProxy$$" + PROXY_COUNTER.getAndIncrement())
                .setAllowPackagePrivate(true);
        if (serviceType.isInterface()) {
            config.addAdditionalInterface(serviceType);
        }
        config.addAdditionalInterface(BytecodeRecorderImpl.ReturnedProxy.class);

        ProxyFactory<T> factory = new ProxyFactory<>(config);
        try {
            return (T) factory.newInstance(new ServiceRecorderProxyHandler(serviceKey, serviceType, staticInit));
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Failed to create service recorder proxy for " + serviceType.getName(), e);
        }
    }

    /**
     * Create a {@code Supplier<T>} proxy that implements {@link BytecodeRecorderImpl.ReturnedProxy}.
     * The recorder framework wraps the loaded service value in a {@code Supplier} at runtime.
     * <p>
     * This is a temporary bridge for build items that expose {@code Supplier<T>}
     * to unconverted recorder-based consumers.
     *
     * @param serviceType the service type
     * @param serviceName the optional service name
     * @param staticInit whether this targets the static-init phase
     * @return a proxy implementing both {@code Supplier} and {@code ReturnedProxy}
     */
    private Object createSupplierProxy(Class<?> serviceType, String serviceName, boolean staticInit) {
        List<String> serviceNameParts = serviceName == null ? List.of() : List.of(serviceName);
        String serviceKey = LambdaTransliterator.serviceKey(serviceType, serviceNameParts);

        if (staticInit) {
            trackCrossPhaseKey(serviceKey);
        }

        ProxyConfiguration<Supplier> config = new ProxyConfiguration<Supplier>()
                .setAnchorClass(ActionBuilderImpl.class)
                .setClassLoader(Thread.currentThread().getContextClassLoader())
                .setSuperClass(Object.class)
                .setProxyNameSuffix("$$SupplierRecorderProxy$$" + PROXY_COUNTER.getAndIncrement())
                .addAdditionalInterface(Supplier.class)
                .addAdditionalInterface(BytecodeRecorderImpl.ReturnedProxy.class);
        ProxyFactory<Supplier> factory = new ProxyFactory<>(config);
        try {
            return factory.newInstance(new SupplierRecorderProxyHandler(serviceKey, serviceType, staticInit));
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Failed to create supplier recorder proxy for " + serviceType.getName(), e);
        }
    }

    /**
     * Record a service value key that must survive between static-init and runtime-init.
     *
     * @param key the cross-phase service key
     */
    private void trackCrossPhaseKey(String key) {
        retentionProducer.accept(new ServiceValueRetentionBuildItem(Set.of(key), false));
    }

    /**
     * Counter used to generate unique proxy class name suffixes.
     */
    private static final AtomicInteger PROXY_COUNTER = new AtomicInteger();

    /**
     * A {@link RuntimeValue} subclass that implements {@link BytecodeRecorderImpl.ReturnedProxy}.
     * <p>
     * At deployment time, this proxy is passed to recorder methods. The recorder sees
     * it as a {@code ReturnedProxy} and generates bytecode that retrieves the actual
     * {@code RuntimeValue} wrapper from the startup context at the stored key.
     *
     * @param <T> the service type
     */
    private static final class ServiceRuntimeValueProxy<T> extends RuntimeValue<T>
            implements BytecodeRecorderImpl.ReturnedProxy {
        private final String rvKey;
        private final boolean staticInit;

        /**
         * Construct a new instance.
         *
         * @param rvKey the startup context key for the {@code RuntimeValue} wrapper
         * @param staticInit whether this proxy targets the static-init phase
         */
        ServiceRuntimeValueProxy(String rvKey, boolean staticInit) {
            this.rvKey = rvKey;
            this.staticInit = staticInit;
        }

        public String __returned$proxy$key() {
            return rvKey;
        }

        public boolean __static$$init() {
            return staticInit;
        }

        @Override
        public boolean __service$$value() {
            return true;
        }
    }

    /**
     * Invocation handler for bare service recorder proxies.
     * <p>
     * Handles the {@link BytecodeRecorderImpl.ReturnedProxy} methods, plus
     * {@code toString}, {@code hashCode}, and {@code equals}. All other method
     * invocations throw, since the proxy exists only for deployment-time recording.
     */
    private static final class ServiceRecorderProxyHandler implements InvocationHandler {
        private final String serviceKey;
        private final Class<?> serviceType;
        private final boolean staticInit;

        /**
         * Construct a new instance.
         *
         * @param serviceKey the startup context key for the service value
         * @param serviceType the service type (for diagnostics)
         * @param staticInit whether this proxy targets the static-init phase
         */
        ServiceRecorderProxyHandler(String serviceKey, Class<?> serviceType, boolean staticInit) {
            this.serviceKey = serviceKey;
            this.serviceType = serviceType;
            this.staticInit = staticInit;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "__returned$proxy$key" -> serviceKey;
                case "__static$$init" -> staticInit;
                case "__service$$value" -> true;
                case "__supplier$$wrapper" -> false;
                case "toString" -> "Service recorder proxy of " + serviceType.getName() + " with key " + serviceKey;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new RuntimeException(
                        "You cannot invoke " + method.getName()
                                + "() directly on a service recorder proxy, "
                                + "you can only pass it as a parameter to a @Recorder method");
            };
        }
    }

    /**
     * Invocation handler for {@code Supplier<T>} service recorder proxies.
     * <p>
     * Like {@link ServiceRecorderProxyHandler}, but also implements
     * {@code __supplier$$wrapper()} returning {@code true}, causing the recorder
     * framework to wrap the loaded service value in a {@code Supplier} at runtime.
     * <p>
     * This is a temporary bridge for the recorder coexistence period.
     */
    private static final class SupplierRecorderProxyHandler implements InvocationHandler {
        private final String serviceKey;
        private final Class<?> serviceType;
        private final boolean staticInit;

        /**
         * Construct a new instance.
         *
         * @param serviceKey the startup context key for the service value
         * @param serviceType the service type (for diagnostics)
         * @param staticInit whether this proxy targets the static-init phase
         */
        SupplierRecorderProxyHandler(String serviceKey, Class<?> serviceType, boolean staticInit) {
            this.serviceKey = serviceKey;
            this.serviceType = serviceType;
            this.staticInit = staticInit;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "__returned$proxy$key" -> serviceKey;
                case "__static$$init" -> staticInit;
                case "__service$$value" -> true;
                case "__supplier$$wrapper" -> true;
                case "toString" -> "Service supplier proxy of " + serviceType.getName() + " with key " + serviceKey;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new RuntimeException(
                        "You cannot invoke " + method.getName()
                                + "() directly on a service supplier proxy, "
                                + "you can only pass it as a parameter to a @Recorder method");
            };
        }
    }
}
