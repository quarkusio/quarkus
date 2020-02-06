package io.quarkus.deployment.builditem;

import java.time.Instant;
import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ApplicationInfoBuildItem extends SimpleBuildItem {

    public static final String UNSET_VALUE = "<<unset>>";

    private final String name;
    private final String version;
    private final Instant buildTime;

    public ApplicationInfoBuildItem(Optional<String> name, Optional<String> version, Optional<Instant> buildTime) {
        this.name = name.orElse(UNSET_VALUE);
        this.version = version.orElse(UNSET_VALUE);
        this.buildTime = buildTime.orElseGet(() -> Instant.now());
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    /**
     * An {@link Instant} to use as a build time. It does not necessarily have to be the clock time at the time when the
     * application is built. This can also be some stable value for the sake of reproducibility.
     *
     * @return an {@link Instant} to use as a build time.
     */
    public Instant getBuildTime() {
        return buildTime;
    }
}
