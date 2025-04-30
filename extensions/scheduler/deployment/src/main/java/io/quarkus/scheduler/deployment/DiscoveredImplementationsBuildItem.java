package io.quarkus.scheduler.deployment;

import java.util.Objects;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.scheduler.runtime.CompositeScheduler;
import io.quarkus.scheduler.runtime.Constituent;
import io.quarkus.scheduler.runtime.SchedulerConfig;
import io.smallrye.common.annotation.Identifier;

/**
 * This build item holds all discovered {@link io.quarkus.scheduler.Scheduler} implementations sorted by priority. Higher
 * priority goes first.
 */
public final class DiscoveredImplementationsBuildItem extends SimpleBuildItem {

    private final String autoImplementation;

    private final Set<String> implementations;

    private final boolean useCompositeScheduler;

    DiscoveredImplementationsBuildItem(String autoImplementation, Set<String> implementations, boolean useCompositeScheduler) {
        this.autoImplementation = Objects.requireNonNull(autoImplementation);
        this.implementations = Objects.requireNonNull(implementations);
        this.useCompositeScheduler = useCompositeScheduler;
    }

    /**
     *
     * @return the implementation with highest priority
     */
    public String getAutoImplementation() {
        return autoImplementation;
    }

    public Set<String> getImplementations() {
        return implementations;
    }

    /**
     * A composite scheduler is used if multiple scheduler implementations are found and
     * {@link SchedulerConfig#useCompositeScheduler} is set to {@code true}.
     * <p>
     * The extension will add:
     * <ul>
     * <li>the {@link Constituent} marker qualifier,</li>
     * <li>the {@link Identifier} qualifier with the corresponding implementation value.</li>
     * </ul>
     *
     * @return {@code true} if a composite scheduler is used
     * @see CompositeScheduler
     */
    public boolean isCompositeSchedulerUsed() {
        return useCompositeScheduler && implementations.size() > 1;
    }

    public boolean isAutoImplementation(String implementation) {
        return autoImplementation.equals(implementation);
    }

}
