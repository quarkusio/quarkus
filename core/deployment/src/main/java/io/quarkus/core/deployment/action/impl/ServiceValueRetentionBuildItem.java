package io.quarkus.core.deployment.action.impl;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Declares service value keys that must survive beyond the phase that produced them.
 * <p>
 * Instances are produced by:
 * <ul>
 * <li>{@link ActionBuilderImpl} — for bare proxy keys created by
 * {@code staticInitServiceAsRecorderValue()} which are consumed by
 * runtime-init recorders</li>
 * <li>{@code SyntheticBeansProcessor} — for CDI service-value bean keys
 * that are consumed lazily after startup</li>
 * </ul>
 * <p>
 * {@code MainClassBuildStep} consumes all instances and generates
 * {@code retainServiceValues()} calls at phase boundaries to discard
 * keys that are no longer needed.
 */
public final class ServiceValueRetentionBuildItem extends MultiBuildItem {

    private final Set<String> keys;
    private final boolean neededAfterStartup;

    /**
     * Construct a new instance.
     *
     * @param keys the service value keys that must be retained (must not be {@code null})
     * @param neededAfterStartup {@code true} if these keys are consumed lazily after startup
     *        (e.g., CDI service-value beans), {@code false} if they are only needed
     *        during the runtime-init deploy phase
     */
    public ServiceValueRetentionBuildItem(Set<String> keys, boolean neededAfterStartup) {
        this.keys = Set.copyOf(keys);
        this.neededAfterStartup = neededAfterStartup;
    }

    /**
     * Get the service value keys.
     *
     * @return the keys (not {@code null}, immutable)
     */
    public Set<String> keys() {
        return keys;
    }

    /**
     * Whether these keys are needed after startup (lazily consumed by CDI beans).
     *
     * @return {@code true} if needed after startup
     */
    public boolean neededAfterStartup() {
        return neededAfterStartup;
    }
}
