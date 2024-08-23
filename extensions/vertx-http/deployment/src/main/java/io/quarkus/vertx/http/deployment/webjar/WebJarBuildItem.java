package io.quarkus.vertx.http.deployment.webjar;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.GACT;

/**
 * BuildItem for deploying a webjar.
 */
public final class WebJarBuildItem extends MultiBuildItem {
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

    private final WebJarResourcesFilter filter;

    private WebJarBuildItem(Builder builder) {
        this.artifactKey = builder.artifactKey;
        this.root = builder.root;
        this.useDefaultQuarkusBranding = builder.useDefaultQuarkusBranding;
        this.onlyCopyNonArtifactFiles = builder.onlyCopyNonArtifactFiles;
        this.filter = builder.filter;
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

    public WebJarResourcesFilter getFilter() {
        return filter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private GACT artifactKey;
        private String root;
        private WebJarResourcesFilter filter;
        private boolean useDefaultQuarkusBranding = true;
        private boolean onlyCopyNonArtifactFiles = true;

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

        public Builder filter(WebJarResourcesFilter filter) {
            this.filter = filter;
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

        public WebJarBuildItem build() {
            return new WebJarBuildItem(this);
        }
    }
}
