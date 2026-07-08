package io.quarkus.core.deployment.action;

import java.util.List;
import java.util.function.Supplier;

import io.quarkus.deployment.Phase;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.RuntimeValue;

/**
 * The entry point for defining services from build steps.
 * <p>
 * An {@code ActionBuilder} is injected as a parameter into {@code @BuildStep} methods.
 * It provides a fluent API for defining services that execute at application startup.
 * Services are assigned to a {@link Phase} via {@code atPhase()} on the builder;
 * the default phase is {@link Phase#APPLICATION}.
 * <h2>Dependency ordering during recorder coexistence</h2>
 * During the transitional period where bytecode recorders and services coexist,
 * service execution order is determined by build step ordering (via {@code @Consume}/
 * {@code @Produce} annotations on build items), not by the service dependency graph.
 * Service dependencies declared via {@code require()}, {@code after()}, etc. are
 * validated against the build step ordering for consistency, but this validation
 * may not detect all missing service dependencies.
 * <p>
 * Once bytecode recorders have been fully removed, the bytecode generator will be
 * rewritten to use strictly dependency-based ordering, and the build step ordering
 * constraints will no longer be required.
 * <p>
 * Until then, extension authors should declare both the build step ordering
 * ({@code @Consume}/{@code @Produce}) and the service dependencies ({@code require()}/
 * {@code after()}) to ensure a smooth transition.
 */
public interface ActionBuilder {

    /**
     * Begin defining a typed service with the given type and no name.
     * The default phase is {@link Phase#APPLICATION}; use {@code atPhase()} to change it.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param <T> the service type
     * @return a service builder with zero dependencies
     */
    <T> ServiceBuilder0<T> forService(Class<T> serviceType);

    /**
     * Begin defining a named typed service.
     * The default phase is {@link Phase#APPLICATION}; use {@code atPhase()} to change it.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param serviceName the service name (may be {@code null} for an unnamed service)
     * @param <T> the service type
     * @return a service builder with zero dependencies
     */
    <T> ServiceBuilder0<T> forService(Class<T> serviceType, String serviceName);

    /**
     * Begin defining a named typed service with multi-part name.
     * The default phase is {@link Phase#APPLICATION}; use {@code atPhase()} to change it.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param serviceNameParts the service name parts (must not be {@code null})
     * @param <T> the service type
     * @return a service builder with zero dependencies
     */
    <T> ServiceBuilder0<T> forService(Class<T> serviceType, List<String> serviceNameParts);

    /**
     * Begin defining a void service with the given name.
     * Void services produce no value and are identified by name alone.
     * The default phase is {@link Phase#APPLICATION}; use {@code atPhase()} to change it.
     *
     * @param serviceName the service name (must not be {@code null})
     * @return a void service builder with zero dependencies
     */
    VoidServiceBuilder0 forService(String serviceName);

    /**
     * Begin defining a void service with the given multi-part name.
     * Void services produce no value and are identified by name alone.
     * The default phase is {@link Phase#APPLICATION}; use {@code atPhase()} to change it.
     *
     * @param serviceNameParts the service name parts (must not be {@code null})
     * @return a void service builder with zero dependencies
     */
    VoidServiceBuilder0 forService(List<String> serviceNameParts);

    /**
     * Publish a value produced by the legacy {@code @Recorder} system as a named service,
     * making it available as a dependency for ActionBuilder-based services via {@code require()}.
     * <p>
     * The {@code recorderValue} must be a recorder proxy (an object returned from a
     * {@code @Recorder} method). The phase (static-init vs runtime) is determined
     * automatically from the proxy.
     * <p>
     * This is a temporary bridge to allow incremental migration from the recorder
     * system to the ActionBuilder system.
     *
     * @param serviceType the service type to register the value under (must not be {@code null})
     * @param recorderValue the recorder-produced proxy value (must not be {@code null})
     * @param <T> the service type
     */
    <T> void aliasRecorderValue(Class<T> serviceType, T recorderValue);

    /**
     * Publish a value produced by the legacy {@code @Recorder} system as an unnamed service
     * in a specific runtime phase,
     * making it available as a dependency for ActionBuilder-based services via {@code require()}.
     * <p>
     * The {@code recorderValue} must be a recorder proxy (an object returned from a
     * {@code @Recorder} method). The phase (static-init vs runtime) is determined
     * automatically from the proxy.
     * <p>
     * This is a temporary bridge to allow incremental migration from the recorder
     * system to the ActionBuilder system.
     *
     * @param serviceType the service type to register the value under (must not be {@code null})
     * @param recorderValue the recorder-produced proxy value (must not be {@code null})
     * @param phase the runtime phase for the alias (must not be {@code null})
     * @param <T> the service type
     */
    <T> void aliasRecorderValue(Class<T> serviceType, T recorderValue, Phase phase);

    /**
     * Publish a value produced by the legacy {@code @Recorder} system as a named service,
     * making it available as a dependency for ActionBuilder-based services via {@code require()}.
     * <p>
     * The {@code recorderValue} must be a recorder proxy (an object returned from a
     * {@code @Recorder} method). The phase (static-init vs runtime) is determined
     * automatically from the proxy.
     * <p>
     * This is a temporary bridge to allow incremental migration from the recorder
     * system to the ActionBuilder system.
     *
     * @param serviceType the service type to register the value under (must not be {@code null})
     * @param serviceName the service name (may be {@code null} for an unnamed service)
     * @param recorderValue the recorder-produced proxy value (must not be {@code null})
     * @param <T> the service type
     */
    <T> void aliasRecorderValue(Class<T> serviceType, String serviceName, T recorderValue);

    /**
     * Publish a value produced by the legacy {@code @Recorder} system as a named service
     * in a specific runtime phase,
     * making it available as a dependency for ActionBuilder-based services via {@code require()}.
     * <p>
     * The {@code recorderValue} must be a recorder proxy (an object returned from a
     * {@code @Recorder} method). The phase (static-init vs runtime) is determined
     * automatically from the proxy.
     * <p>
     * This is a temporary bridge to allow incremental migration from the recorder
     * system to the ActionBuilder system.
     *
     * @param serviceType the service type to register the value under (must not be {@code null})
     * @param serviceName the service name (may be {@code null} for an unnamed service)
     * @param recorderValue the recorder-produced proxy value (must not be {@code null})
     * @param phase the runtime phase for the alias (must not be {@code null})
     * @param <T> the service type
     */
    <T> void aliasRecorderValue(Class<T> serviceType, String serviceName, T recorderValue, Phase phase);

    // ── Service → Recorder bridge methods ──

    /**
     * Produce a {@link RuntimeValue RuntimeValue&lt;T&gt;} proxy
     * for a runtime service, suitable for passing to legacy {@code @Record(RUNTIME_INIT)}
     * recorder methods or embedding in legacy build items (e.g. {@code VertxBuildItem}).
     * <p>
     * The returned object extends {@code RuntimeValue} and implements
     * {@link BytecodeRecorderImpl.ReturnedProxy ReturnedProxy}.
     * At runtime, the recorder-generated code retrieves the wrapper from the startup context;
     * the wrapper contains the actual service value.
     * <p>
     * The service must be defined (via {@link #forService(Class)}) before or during the same
     * build step.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param <T> the service type
     * @return a {@code RuntimeValue<T>} proxy for use with legacy recorders
     */
    <T> RuntimeValue<T> serviceAsRuntimeValue(Class<T> serviceType);

    /**
     * Produce a {@link RuntimeValue RuntimeValue&lt;T&gt;} proxy
     * for a named runtime service.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param serviceName the service name (may be {@code null} for an unnamed service)
     * @param <T> the service type
     * @return a {@code RuntimeValue<T>} proxy for use with legacy recorders
     * @see #serviceAsRuntimeValue(Class)
     */
    <T> RuntimeValue<T> serviceAsRuntimeValue(Class<T> serviceType, String serviceName);

    /**
     * Produce a {@link RuntimeValue RuntimeValue&lt;T&gt;} proxy
     * for a runtime service with a multi-part name.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param serviceNameParts the service name parts (must not be {@code null})
     * @param <T> the service type
     * @return a {@code RuntimeValue<T>} proxy for use with legacy recorders
     * @see #serviceAsRuntimeValue(Class)
     */
    <T> RuntimeValue<T> serviceAsRuntimeValue(Class<T> serviceType, List<String> serviceNameParts);

    /**
     * Produce a bare {@code T} proxy for a runtime service, suitable for passing
     * directly as a parameter to legacy {@code @Record(RUNTIME_INIT)} recorder methods.
     * <p>
     * The returned proxy implements
     * {@link BytecodeRecorderImpl.ReturnedProxy ReturnedProxy}
     * and the service type. When the recorder sees this proxy as a method argument,
     * it generates code to load the service value from the startup context.
     * <p>
     * The service must be defined (via {@link #forService(Class)}) before or during the same
     * build step.
     *
     * @param serviceType the service type (must not be {@code null}; must be proxyable)
     * @param <T> the service type
     * @return a {@code T} proxy for use with legacy recorders
     */
    <T> T serviceAsRecorderValue(Class<T> serviceType);

    /**
     * Produce a bare {@code T} proxy for a named runtime service.
     *
     * @param serviceType the service type (must not be {@code null}; must be proxyable)
     * @param serviceName the service name (may be {@code null} for an unnamed service)
     * @param <T> the service type
     * @return a {@code T} proxy for use with legacy recorders
     * @see #serviceAsRecorderValue(Class)
     */
    <T> T serviceAsRecorderValue(Class<T> serviceType, String serviceName);

    // ── Service → Supplier bridge (temporary) ──

    /**
     * Produce a {@link java.util.function.Supplier Supplier&lt;T&gt;} proxy
     * for a runtime service, suitable for passing to legacy recorder methods or
     * embedding in build items that expose {@code Supplier<T>}.
     * <p>
     * At runtime, the generated bytecode loads the service value and wraps it
     * in a {@code Supplier}. The returned proxy implements
     * {@link BytecodeRecorderImpl.ReturnedProxy ReturnedProxy}.
     * <p>
     * This is a temporary bridge for the recorder coexistence period. Once all
     * consumers are converted to use {@code require()}, this method and the
     * build items that use it should be removed.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param <T> the service type
     * @return a {@code Supplier<T>} proxy for use with legacy recorders and build items
     */
    <T> Supplier<T> serviceAsRecorderSupplier(Class<T> serviceType);

    /**
     * Produce a {@link java.util.function.Supplier Supplier&lt;T&gt;} proxy
     * for a named runtime service.
     * <p>
     * This is a temporary bridge for the recorder coexistence period.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param serviceName the service name (may be {@code null} for an unnamed service)
     * @param <T> the service type
     * @return a {@code Supplier<T>} proxy for use with legacy recorders and build items
     * @see #serviceAsRecorderSupplier(Class)
     */
    <T> Supplier<T> serviceAsRecorderSupplier(Class<T> serviceType, String serviceName);

    // ── Static-init → Recorder bridge methods ──

    /**
     * Produce a {@link RuntimeValue RuntimeValue&lt;T&gt;} proxy
     * for a static-init service, suitable for passing to legacy
     * {@code @Record(STATIC_INIT)} or {@code @Record(RUNTIME_INIT)} recorder methods.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param <T> the service type
     * @return a {@code RuntimeValue<T>} proxy for use with legacy recorders
     * @see #serviceAsRuntimeValue(Class)
     */
    <T> RuntimeValue<T> staticInitServiceAsRuntimeValue(Class<T> serviceType);

    /**
     * Produce a {@link RuntimeValue RuntimeValue&lt;T&gt;} proxy
     * for a named static-init service.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param serviceName the service name (may be {@code null} for an unnamed service)
     * @param <T> the service type
     * @return a {@code RuntimeValue<T>} proxy for use with legacy recorders
     * @see #staticInitServiceAsRuntimeValue(Class)
     */
    <T> RuntimeValue<T> staticInitServiceAsRuntimeValue(Class<T> serviceType, String serviceName);

    /**
     * Produce a {@link RuntimeValue RuntimeValue&lt;T&gt;} proxy
     * for a static-init service with a multi-part name.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param serviceNameParts the service name parts (must not be {@code null})
     * @param <T> the service type
     * @return a {@code RuntimeValue<T>} proxy for use with legacy recorders
     * @see #staticInitServiceAsRuntimeValue(Class)
     */
    <T> RuntimeValue<T> staticInitServiceAsRuntimeValue(Class<T> serviceType, List<String> serviceNameParts);

    /**
     * Produce a bare {@code T} proxy for a static-init service, suitable for passing
     * directly as a parameter to legacy {@code @Record(STATIC_INIT)} or
     * {@code @Record(RUNTIME_INIT)} recorder methods.
     *
     * @param serviceType the service type (must not be {@code null}; must be proxyable)
     * @param <T> the service type
     * @return a {@code T} proxy for use with legacy recorders
     * @see #serviceAsRecorderValue(Class)
     */
    <T> T staticInitServiceAsRecorderValue(Class<T> serviceType);

    /**
     * Produce a bare {@code T} proxy for a named static-init service.
     *
     * @param serviceType the service type (must not be {@code null}; must be proxyable)
     * @param serviceName the service name (may be {@code null} for an unnamed service)
     * @param <T> the service type
     * @return a {@code T} proxy for use with legacy recorders
     * @see #staticInitServiceAsRecorderValue(Class)
     */
    <T> T staticInitServiceAsRecorderValue(Class<T> serviceType, String serviceName);
}
