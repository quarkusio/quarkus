package io.quarkus.devconsole.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.GACT;

/**
 * @deprecated as part of the removal of the old Dev UI
 */
@Deprecated
public final class DevConsoleWebjarBuildItem extends MultiBuildItem {
    /**
     * ArtifactKey pointing to the web jar. Has to be one of the applications dependencies.
     */
    private final GACT artifactKey;

    /**
     * Root inside the webJar starting from which resources are unpacked.
     */
    private final String root;

    /**
     * Only copy resources of the webjar which are either user overridden, or contain variables.
     */
    private final boolean onlyCopyNonArtifactFiles;

    /**
     * Defines whether Quarkus can override resources of the webjar with Quarkus internal files.
     */
    private final boolean useDefaultQuarkusBranding;

    /**
     * The root of the route to expose resources of the webjar
     */
    private final String routeRoot;

    private DevConsoleWebjarBuildItem(Builder builder) {
        this.artifactKey = builder.artifactKey;
        this.root = builder.root;
        this.useDefaultQuarkusBranding = builder.useDefaultQuarkusBranding;
        this.onlyCopyNonArtifactFiles = builder.onlyCopyNonArtifactFiles;
        this.routeRoot = builder.routeRoot;
    }

    public GACT getArtifactKey() {
        return artifactKey;
    }

    public String getRoot() {
        return root;
    }

    public boolean getUseDefaultQuarkusBranding() {
        return useDefaultQuarkusBranding;
    }

    public boolean getOnlyCopyNonArtifactFiles() {
        return onlyCopyNonArtifactFiles;
    }

    public String getRouteRoot() {
        return routeRoot;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private GACT artifactKey;
        private String root;
        private boolean useDefaultQuarkusBranding = true;
        private boolean onlyCopyNonArtifactFiles = true;
        private String routeRoot;

        public Builder artifactKey(GACT artifactKey) {
            this.artifactKey = artifactKey;
            return this;
        }

        public Builder root(String root) {
            this.root = root;

            if (this.root != null && this.root.startsWith("/")) {
                this.root = this.root.substring(1);
            }

            return this;
        }

        public Builder routeRoot(String route) {
            this.routeRoot = route;
            return this;
        }

        public Builder useDefaultQuarkusBranding(boolean useDefaultQuarkusBranding) {
            this.useDefaultQuarkusBranding = useDefaultQuarkusBranding;
            return this;
        }

        public Builder onlyCopyNonArtifactFiles(boolean onlyCopyNonArtifactFiles) {
            this.onlyCopyNonArtifactFiles = onlyCopyNonArtifactFiles;
            return this;
        }

        public DevConsoleWebjarBuildItem build() {
            return new DevConsoleWebjarBuildItem(this);
        }
    }
}
