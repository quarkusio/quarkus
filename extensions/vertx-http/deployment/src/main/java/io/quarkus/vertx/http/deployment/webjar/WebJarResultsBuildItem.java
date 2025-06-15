package io.quarkus.vertx.http.deployment.webjar;

import java.util.List;
import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;

/**
 * Holds the complete result after applying every {@link WebJarBuildItem}.
 */
public final class WebJarResultsBuildItem extends SimpleBuildItem {
    private final Map<GACT, WebJarResult> results;

    public WebJarResultsBuildItem(Map<GACT, WebJarResult> results) {
        this.results = results;
    }

    public WebJarResult byArtifactKey(GACT artifactKey) {
        return results.get(artifactKey);
    }

    public static class WebJarResult {
        /**
         * Resolved dependency of the webjar
         */
        private final ResolvedDependency dependency;

        /**
         * Path to where the webjar content was unpacked to. For dev and test mode, the files while be unpacked to a
         * temp directory on disk. In Prod Mode, the files will be available as generated resources inside this path.
         */
        private final String finalDestination;

        /**
         * Web roots that can be used to serve web jar files from.
         */
        private final List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations;

        public WebJarResult(ResolvedDependency dependency, String finalDestination,
                List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations) {
            this.dependency = dependency;
            this.finalDestination = finalDestination;
            this.webRootConfigurations = webRootConfigurations;
        }

        public ResolvedDependency getDependency() {
            return dependency;
        }

        public String getFinalDestination() {
            return finalDestination;
        }

        public List<FileSystemStaticHandler.StaticWebRootConfiguration> getWebRootConfigurations() {
            return webRootConfigurations;
        }
    }
}
