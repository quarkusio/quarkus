package io.quarkus.devjsonrpc.spi;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Resolves the namespace of an extension in the Dev UI, which is the artifactId of its runtime artifact, from the
 * artifact that declares the Dev UI build items, which is its deployment artifact.
 * <p>
 * The two are linked by the {@code deployment-artifact} property of the runtime artifact's extension descriptor.
 * Relying on the {@code -deployment} naming convention alone breaks extensions whose deployment artifact is named
 * differently, for example a runtime {@code framework-core} with a deployment {@code framework-deployment}.
 */
public final class ExtensionNamespaces {

    private static final String DEPLOYMENT_SUFFIX = "-deployment";

    private static final Map<ApplicationModel, Map<ArtifactKey, String>> CACHE = Collections
            .synchronizedMap(new WeakHashMap<>());

    private ExtensionNamespaces() {
    }

    /**
     * @param deploymentKey the artifact the Dev UI build item was created from, with the {@code -deployment} suffix
     *        already removed as {@code ArtifactInfoUtil} does
     * @return the artifactId of the runtime artifact whose descriptor points at that deployment artifact, or the
     *         artifactId of the given key when no descriptor does
     */
    public static String runtimeArtifactId(CurateOutcomeBuildItem curateOutcomeBuildItem, ArtifactKey deploymentKey) {
        ApplicationModel model = curateOutcomeBuildItem.getApplicationModel();
        Map<ArtifactKey, String> mapping = CACHE.computeIfAbsent(model, ExtensionNamespaces::deploymentToRuntime);
        return mapping.getOrDefault(ArtifactKey.ga(deploymentKey.getGroupId(), deploymentKey.getArtifactId()),
                deploymentKey.getArtifactId());
    }

    private static Map<ArtifactKey, String> deploymentToRuntime(ApplicationModel model) {
        Map<ArtifactKey, String> mapping = new HashMap<>();
        for (ResolvedDependency runtimeExtension : model.getDependencies(DependencyFlags.RUNTIME_EXTENSION_ARTIFACT)) {
            String deploymentCoords = runtimeExtension.getContentTree().apply(BootstrapConstants.DESCRIPTOR_PATH,
                    visit -> {
                        if (visit == null) {
                            return null;
                        }
                        Properties props = new Properties();
                        try (BufferedReader reader = Files.newBufferedReader(visit.getPath())) {
                            props.load(reader);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to read " + visit.getUrl(), e);
                        }
                        return props.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
                    });
            ArtifactKey deploymentKey = deploymentKey(deploymentCoords);
            if (deploymentKey != null) {
                mapping.putIfAbsent(deploymentKey, runtimeExtension.getArtifactId());
            }
        }
        return mapping;
    }

    /**
     * @param deploymentCoords the {@code deployment-artifact} coordinates of an extension descriptor
     * @return the key the build items of that deployment artifact are resolved to (groupId and artifactId without
     *         the {@code -deployment} suffix), or {@code null} when there are no coordinates
     */
    static ArtifactKey deploymentKey(String deploymentCoords) {
        if (deploymentCoords == null || deploymentCoords.isBlank()) {
            return null;
        }
        GACTV coords = GACTV.fromString(deploymentCoords);
        return ArtifactKey.ga(coords.getGroupId(), stripDeploymentSuffix(coords.getArtifactId()));
    }

    static String stripDeploymentSuffix(String artifactId) {
        if (artifactId.endsWith(DEPLOYMENT_SUFFIX)) {
            return artifactId.substring(0, artifactId.length() - DEPLOYMENT_SUFFIX.length());
        }
        return artifactId;
    }
}
