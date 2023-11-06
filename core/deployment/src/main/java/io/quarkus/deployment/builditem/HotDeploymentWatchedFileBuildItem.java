package io.quarkus.deployment.builditem;

import java.util.function.Predicate;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Identifies a file from a
 * {@link io.quarkus.bootstrap.devmode.DependenciesFilter#getReloadableModules(io.quarkus.bootstrap.model.ApplicationModel)
 * reloadable module} that, if modified, may result in a hot redeployment when in the dev mode.
 * <p>
 * A file may be identified with an location or a matching predicate. See {@link Builder#setLocation(String)} and
 * {@link Builder#setLocationPredicate(Predicate)}.
 *
 * The location may be:
 * <ul>
 * <li>a relative OS-agnostic file path where {@code /} is used as a separator; e.g. {@code foo/bar.txt}</li>
 * <li>an absolute OS-specific file path; e.g. {@code /home/foo/bar.txt}</li>
 * <li>a glob pattern as defined in {@link java.nio.file.FileSystem#getPathMatcher(String)}; e.g. {@code *.sample}</li>
 * </ul>
 * <p>
 * If multiple build items match the same file then the final value of {@code restartNeeded} is computed as a logical OR of all
 * the {@link #isRestartNeeded()} values.
 */
public final class HotDeploymentWatchedFileBuildItem extends MultiBuildItem {

    public static Builder builder() {
        return new Builder();
    }

    private final String location;
    private final Predicate<String> locationPredicate;

    private final boolean restartNeeded;

    /**
     *
     * @param location
     * @see #builder()
     */
    public HotDeploymentWatchedFileBuildItem(String location) {
        this(location, true);
    }

    /**
     *
     * @param location
     * @param restartNeeded
     * @see #builder()
     */
    public HotDeploymentWatchedFileBuildItem(String location, boolean restartNeeded) {
        this(location, null, restartNeeded);
    }

    private HotDeploymentWatchedFileBuildItem(String location, Predicate<String> locationPredicate, boolean restartNeeded) {
        if (location == null && locationPredicate == null) {
            throw new IllegalArgumentException("Either location or predicate must be set");
        }
        this.location = location;
        this.locationPredicate = locationPredicate;
        this.restartNeeded = restartNeeded;
    }

    /**
     *
     * @return a location a file from a reloadable module
     */
    public String getLocation() {
        return location;
    }

    public boolean hasLocation() {
        return location != null;
    }

    /**
     *
     * @return a predicate used to match a file from a reloadable module
     */
    public Predicate<String> getLocationPredicate() {
        return locationPredicate;
    }

    public boolean hasLocationPredicate() {
        return locationPredicate != null;
    }

    /**
     *
     * @return {@code true} if a file change should result in an application restart, {@code false} otherwise
     */
    public boolean isRestartNeeded() {
        return restartNeeded;
    }

    public static class Builder {

        private String location;
        private Predicate<String> locationPredicate;
        private boolean restartNeeded = true;

        public Builder setLocation(String location) {
            if (locationPredicate != null) {
                throw new IllegalArgumentException("Predicate already set");
            }
            this.location = location;
            return this;
        }

        public Builder setLocationPredicate(Predicate<String> locationPredicate) {
            if (location != null) {
                throw new IllegalArgumentException("Location already set");
            }
            this.locationPredicate = locationPredicate;
            return this;
        }

        public Builder setRestartNeeded(boolean restartNeeded) {
            this.restartNeeded = restartNeeded;
            return this;
        }

        public HotDeploymentWatchedFileBuildItem build() {
            return new HotDeploymentWatchedFileBuildItem(location, locationPredicate, restartNeeded);
        }

    }

}
