package io.quarkus.core.deployment.action.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Common implementation for all {@code ServiceBuilderN} arities.
 * <p>
 * This single class backs all service builder interfaces. The type safety is
 * provided by the generated interfaces at the call site; internally, raw types
 * are used where necessary. The builder accumulates dependency identities via
 * {@code require()} and terminates the chain via {@code action()} or
 * {@code actionAsync()}, which triggers lambda extraction and produces the
 * necessary build items.
 * <p>
 * The phase (set via {@code atPhase()}) determines which build items are
 * produced: {@link Phase#STATIC_INIT} routes to static-init items, all other
 * phases route to runtime-init items.
 *
 * @param <T> the service type
 */
final class ServiceBuilderImpl<T> {

    private final Class<T> serviceType;
    private final List<String> serviceNameParts;
    private final Consumer<MainBytecodeRecorderBuildItem> recorderProducer;
    private final Consumer<StaticBytecodeRecorderBuildItem> staticRecorderProducer;
    private final Consumer<ServiceMetadataBuildItem> metadataProducer;
    private final Consumer<StaticServiceMetadataBuildItem> staticMetadataProducer;
    private final String buildStepName;
    private final List<Dependency> dependencies = new ArrayList<>();
    private final List<String> beforeKeys = new ArrayList<>();
    private final List<String> afterBuildItemClasses = new ArrayList<>();
    private Phase phase = Phase.APPLICATION;

    /**
     * Construct the initial builder with zero dependencies.
     *
     * @param serviceType the service type
     * @param serviceNameParts the service name parts
     * @param recorderProducer consumer for runtime-init build items
     * @param staticRecorderProducer consumer for static-init build items
     * @param metadataProducer consumer for runtime-init metadata
     * @param staticMetadataProducer consumer for static-init metadata
     * @param buildStepName the name of the declaring build step
     */
    ServiceBuilderImpl(
            Class<T> serviceType,
            List<String> serviceNameParts,
            Consumer<MainBytecodeRecorderBuildItem> recorderProducer,
            Consumer<StaticBytecodeRecorderBuildItem> staticRecorderProducer,
            Consumer<ServiceMetadataBuildItem> metadataProducer,
            Consumer<StaticServiceMetadataBuildItem> staticMetadataProducer,
            String buildStepName) {
        this.serviceType = serviceType;
        this.serviceNameParts = serviceNameParts;
        this.recorderProducer = recorderProducer;
        this.staticRecorderProducer = staticRecorderProducer;
        this.metadataProducer = metadataProducer;
        this.staticMetadataProducer = staticMetadataProducer;
        this.buildStepName = buildStepName;
    }

    // ── require() ──
    // NOTE: during the recorder coexistence period, deploy method ordering is controlled
    // by build step ordering (@Consume/@Produce), not by the service dependency graph.
    // Service dependencies are validated against the build step ordering, but this
    // validation may not detect all missing dependencies. Ensure the corresponding
    // build step ordering constraints are in place.

    /**
     * Add an unnamed injected dependency.
     *
     * @param type the dependency type (must not be {@code null})
     */
    public void require(Class<?> type) {
        require(type, List.of(), true);
    }

    /**
     * Add a named injected dependency.
     *
     * @param type the dependency type (must not be {@code null})
     * @param name the dependency name (must not be {@code null})
     */
    public void require(Class<?> type, String name) {
        require(type, List.of(name), true);
    }

    /**
     * Add an injected dependency with the given name parts.
     *
     * @param type the dependency type (must not be {@code null})
     * @param name the dependency name parts (must not be {@code null})
     */
    public void require(Class<?> type, List<String> name) {
        require(type, name, true);
    }

    /**
     * Add a dependency with the given name parts and injection flag.
     * <p>
     * If the type is a {@code @ConfigRoot} config mapping, the dependency is resolved
     * directly from SmallRye Config at runtime (no service values map involvement).
     *
     * @param type the dependency type (must not be {@code null})
     * @param name the dependency name parts (must not be {@code null})
     * @param injected {@code true} if this dependency is injected
     */
    public void require(Class<?> type, List<String> name, boolean injected) {
        ConfigRoot configRoot = ConfigMappingDetector.findConfigRoot(type);
        if (configRoot != null) {
            ConfigPhase phase = configRoot.phase();
            if (phase == ConfigPhase.BUILD_TIME) {
                throw new IllegalArgumentException(
                        "Build-time configuration " + type.getName()
                                + " cannot be required by a service action: "
                                + "it is not available at runtime.");
            }
            if (phase == ConfigPhase.RUN_TIME && this.phase == Phase.STATIC_INIT) {
                throw new IllegalArgumentException(
                        "Runtime configuration " + type.getName()
                                + " cannot be required by a static-init service action: "
                                + "it is not available during static initialization.");
            }
            // config-direct: resolved from SmallRye Config, not the service values map
            dependencies.add(new Dependency(type, name, (injected ? Dependency.FL_INJECTED : 0) | Dependency.FL_CONFIG_DIRECT));
            return;
        }
        dependencies.add(new Dependency(type, name, injected ? Dependency.FL_INJECTED : 0));
    }

    // ── request() — optional injected dependencies ──

    /**
     * Add an unnamed optional injected dependency.
     * The dependency is injected as {@code Optional<T>}; if absent, an empty optional is provided.
     *
     * @param type the dependency type (must not be {@code null})
     */
    public void request(Class<?> type) {
        request(type, List.of());
    }

    /**
     * Add a named optional injected dependency.
     *
     * @param type the dependency type (must not be {@code null})
     * @param name the dependency name (must not be {@code null})
     */
    public void request(Class<?> type, String name) {
        request(type, List.of(name));
    }

    /**
     * Add an optional injected dependency with the given name parts.
     *
     * @param type the dependency type (must not be {@code null})
     * @param name the dependency name parts (must not be {@code null})
     */
    public void request(Class<?> type, List<String> name) {
        dependencies.add(new Dependency(type, name, Dependency.FL_INJECTED | Dependency.FL_OPTIONAL));
    }

    // ── consumeAll() — multi-service dependencies ──

    /**
     * Consume all services of the given type as a {@code Map<String, T>}.
     * The map keys are the service names; unnamed services use empty string.
     * Zero matches produces an empty map (always optional).
     *
     * @param type the service type to match (must not be {@code null})
     */
    public void consumeAll(Class<?> type) {
        dependencies.add(
                new Dependency(type, List.of(), Dependency.FL_INJECTED | Dependency.FL_OPTIONAL | Dependency.FL_CONSUME_ALL));
    }

    // ── after() — optional ordering-only dependencies ──
    // NOTE: during the recorder coexistence period, deploy method ordering is still controlled
    // by build step ordering (@Consume/@Produce). Declaring after() deps now ensures correct
    // ordering once recorders are removed and services run in dependency order. Until then,
    // the corresponding @Consume annotation on the build step must be kept as well.

    /**
     * Add an optional ordering-only dependency on a void service with the given name.
     * If the service exists, this service will start after it completes.
     * If the service does not exist, this dependency is silently ignored.
     *
     * @param name the void service name (must not be {@code null})
     */
    public void after(String name) {
        dependencies.add(new Dependency(Void.class, List.of(name), Dependency.FL_OPTIONAL));
    }

    /**
     * Add an optional ordering-only dependency on a void service with the given multi-part name.
     *
     * @param nameParts the void service name parts (must not be {@code null})
     */
    public void after(List<String> nameParts) {
        dependencies.add(new Dependency(Void.class, nameParts, Dependency.FL_OPTIONAL));
    }

    /**
     * Add an optional ordering-only dependency on an unnamed typed service.
     *
     * @param type the dependency type (must not be {@code null})
     */
    public void after(Class<?> type) {
        dependencies.add(new Dependency(type, List.of(), Dependency.FL_OPTIONAL));
    }

    /**
     * Add an optional ordering-only dependency on a named typed service.
     *
     * @param type the dependency type (must not be {@code null})
     * @param name the service name (must not be {@code null})
     */
    public void after(Class<?> type, String name) {
        dependencies.add(new Dependency(type, List.of(name), Dependency.FL_OPTIONAL));
    }

    /**
     * Add an optional ordering-only dependency on a typed service with the given multi-part name.
     *
     * @param type the dependency type (must not be {@code null})
     * @param nameParts the service name parts (must not be {@code null})
     */
    public void after(Class<?> type, List<String> nameParts) {
        dependencies.add(new Dependency(type, nameParts, Dependency.FL_OPTIONAL));
    }

    // ── before() — reverse ordering dependencies ──
    // Declaring before(X) means X depends on this service: this service starts
    // before X and stops after X. This is the inverse of after().

    /**
     * Declare that the given unnamed typed service should start after this
     * service and stop before it. If the target service does not exist,
     * this declaration is silently ignored.
     *
     * @param type the target service type (must not be {@code null})
     */
    public void before(Class<?> type) {
        beforeKeys.add(LambdaTransliterator.serviceKey(type, List.of()));
    }

    /**
     * Declare that the given named typed service should start after this
     * service and stop before it. If the target service does not exist,
     * this declaration is silently ignored.
     *
     * @param type the target service type (must not be {@code null})
     * @param name the service name (must not be {@code null})
     */
    public void before(Class<?> type, String name) {
        beforeKeys.add(LambdaTransliterator.serviceKey(type, List.of(name)));
    }

    /**
     * Declare that the given named typed service should start after this
     * service and stop before it. If the target service does not exist,
     * this declaration is silently ignored.
     *
     * @param type the target service type (must not be {@code null})
     * @param nameParts the service name parts (must not be {@code null})
     */
    public void before(Class<?> type, List<String> nameParts) {
        beforeKeys.add(LambdaTransliterator.serviceKey(type, nameParts));
    }

    /**
     * Declare that the given void service should start after this
     * service and stop before it. If the target service does not exist,
     * this declaration is silently ignored.
     *
     * @param name the void service name (must not be {@code null})
     */
    public void before(String name) {
        beforeKeys.add(LambdaTransliterator.serviceKey(Void.class, List.of(name)));
    }

    // ── afterBuildItem() — build item ordering dependencies ──

    /**
     * Declare that this service must start after the build step that
     * produces the given build item has completed.
     * <p>
     * This is a transitional API for services that coexist with legacy
     * bytecode recorders. It bridges the service dependency model with
     * the build step dependency model by resolving the producing step's
     * nodes (with passthrough resolution) and adding them as ordering
     * dependencies.
     * <p>
     * Once all recorders are converted to services, this method should
     * be replaced with direct service dependencies via {@code require()},
     * {@code after()}, or {@code before()}.
     * <p>
     * If no step produces the given build item, the dependency is
     * silently ignored.
     *
     * @param buildItemClass the build item class (must not be {@code null})
     * @deprecated Transitional API for recorder coexistence. Use direct
     *             service dependencies once the producing recorder is converted.
     */
    @Deprecated(forRemoval = true)
    public void afterBuildItem(Class<? extends BuildItem> buildItemClass) {
        afterBuildItemClasses.add(buildItemClass.getName());
    }

    // ── atPhase() — runtime phase assignment ──

    /**
     * Assign this service to a phase.
     * The default phase is {@link Phase#APPLICATION}.
     *
     * @param phase the phase (must not be {@code null})
     */
    public void atPhase(Phase phase) {
        this.phase = phase;
    }

    /**
     * Extract the lambda and produce the build items.
     * <p>
     * Class generation is deferred; only the extracted action data is produced here.
     *
     * @param action the action lambda (must be serializable)
     * @param async {@code true} for async actions
     */
    public void doAction(Serializable action, boolean async) {
        boolean isStaticInit = phase == Phase.STATIC_INIT;
        if (isStaticInit && async) {
            throw new IllegalStateException(
                    "Static-init services do not support async actions");
        }
        TransliteratedAction.ActionService extracted = LambdaTransliterator.extract(
                action, serviceType, serviceNameParts, dependencies, beforeKeys, afterBuildItemClasses,
                async, isStaticInit, buildStepName);
        if (isStaticInit) {
            staticRecorderProducer.accept(new StaticBytecodeRecorderBuildItem(extracted));
            staticMetadataProducer.accept(new StaticServiceMetadataBuildItem(
                    serviceType, serviceNameParts, dependencies, buildStepName));
        } else {
            recorderProducer.accept(new MainBytecodeRecorderBuildItem(extracted));
            metadataProducer.accept(new ServiceMetadataBuildItem(
                    serviceType, serviceNameParts, dependencies, buildStepName, phase));
        }
    }
}
