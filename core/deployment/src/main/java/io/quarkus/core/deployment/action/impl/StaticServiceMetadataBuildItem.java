package io.quarkus.core.deployment.action.impl;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item carrying metadata about a static-init service defined via
 * {@link ActionBuilderImpl}.
 * <p>
 * Each instance describes a single static-init service registration: the service
 * identity (type + optional name), the identities of its declared dependencies,
 * and the name of the build step that defined it (for diagnostics).
 * <p>
 * All instances are consumed by the service dependency validation step, which
 * validates the static-init dependency graph independently and checks for
 * cross-phase duplicates with the runtime service graph.
 *
 * @see ServiceMetadataBuildItem
 */
public final class StaticServiceMetadataBuildItem extends MultiBuildItem {

    private final Class<?> serviceType;
    private final List<String> serviceNameParts;
    private final List<Dependency> dependencies;
    private final String buildStepName;

    /**
     * Construct a new instance.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param serviceNameParts the service name parts (must not be {@code null})
     * @param dependencies the declared dependencies (must not be {@code null})
     * @param buildStepName the name of the declaring build step (must not be {@code null})
     */
    public StaticServiceMetadataBuildItem(
            Class<?> serviceType,
            List<String> serviceNameParts,
            List<Dependency> dependencies,
            String buildStepName) {
        this.serviceType = serviceType;
        this.serviceNameParts = List.copyOf(serviceNameParts);
        this.dependencies = List.copyOf(dependencies);
        this.buildStepName = buildStepName;
    }

    /**
     * Get the service type.
     *
     * @return the service type (not {@code null})
     */
    public Class<?> serviceType() {
        return serviceType;
    }

    /**
     * Get the service name parts.
     *
     * @return the service name parts (not {@code null})
     */
    public List<String> serviceNameParts() {
        return serviceNameParts;
    }

    /**
     * Get the declared dependencies.
     *
     * @return the dependency list (not {@code null})
     */
    public List<Dependency> dependencies() {
        return dependencies;
    }

    /**
     * Get the name of the build step that defined this service.
     *
     * @return the build step name (not {@code null})
     */
    public String buildStepName() {
        return buildStepName;
    }

    /**
     * Get the service key used for identity and lookup.
     *
     * @return the service key string
     */
    public String serviceKey() {
        return LambdaTransliterator.serviceKey(serviceType, serviceNameParts);
    }
}
