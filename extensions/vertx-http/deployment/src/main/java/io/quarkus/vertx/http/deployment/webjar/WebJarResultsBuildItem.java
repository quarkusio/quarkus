package io.quarkus.vertx.http.deployment.webjar;

import java.util.ArrayList;
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

    /**
     * Identifies a {@link WebJarBuildItem}: the same artifact can be deployed with several roots.
     */
    public record Key(GACT artifactKey, String root) {

        public Key(GACT artifactKey, String root) {
            this.artifactKey = artifactKey;
            this.root = normalizeRoot(root);
        }

        private static String normalizeRoot(String root) {
            if (root == null) {
                return "";
            }
            if (root.startsWith("/")) {
                root = root.substring(1);
            }
            if (root.endsWith("/")) {
                root = root.substring(0, root.length() - 1);
            }
            return root;
        }
    }

    private final Map<Key, WebJarResult> results;

    public WebJarResultsBuildItem(Map<Key, WebJarResult> results) {
        this.results = results;
    }

    /**
     * @return the result of the {@link WebJarBuildItem} deployed for this artifact and root, or {@code null}
     */
    public WebJarResult byArtifactKeyAndRoot(GACT artifactKey, String root) {
        return results.get(new Key(artifactKey, root));
    }

    /**
     * @return the result of the single {@link WebJarBuildItem} deployed for this artifact, or {@code null}
     * @throws IllegalStateException when the artifact was deployed with several roots; use
     *         {@link #byArtifactKeyAndRoot(GACT, String)} in that case
     */
    public WebJarResult byArtifactKey(GACT artifactKey) {
        WebJarResult found = null;
        List<String> roots = new ArrayList<>();
        for (Map.Entry<Key, WebJarResult> entry : results.entrySet()) {
            if (entry.getKey().artifactKey().equals(artifactKey)) {
                found = entry.getValue();
                roots.add(entry.getKey().root());
            }
        }
        if (roots.size() > 1) {
            throw new IllegalStateException("The web jar " + artifactKey + " was deployed with several roots " + roots
                    + ", use byArtifactKeyAndRoot to select one of them");
        }
        return found;
    }

    public static class WebJarResult {
        /**
         * Resolved dependency of the webjar
         */
        private final ResolvedDependency dependency;

        /**
         * Path to where the webjar content was unpacked to. For dev and test mode, the files while be unpacked to a temp
         * directory on disk. In Prod Mode, the files will be available as generated resources inside this path.
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
