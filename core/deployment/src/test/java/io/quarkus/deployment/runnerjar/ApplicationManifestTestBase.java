package io.quarkus.deployment.runnerjar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.sbom.ApplicationManifestsBuildItem;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.sbom.ApplicationComponent;

public abstract class ApplicationManifestTestBase extends BootstrapFromOriginalJarTestBase {

    private static String getComponentKey(ApplicationComponent comp) {
        return comp.getResolvedDependency() == null ? comp.getDistributionPath()
                : comp.getResolvedDependency().toCompactCoords();
    }

    protected static ArtifactCoords artifactCoords(String artifactId) {
        return ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, artifactId, TsArtifact.DEFAULT_VERSION);
    }

    protected static ArtifactCoords artifactCoords(String artifactId, String classifier) {
        return ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, artifactId, classifier, TsArtifact.DEFAULT_VERSION);
    }

    private final Map<String, Consumer<ApplicationComponent>> expectedComponents = new HashMap<>();

    private void expectComponent(String compKey, Consumer<ApplicationComponent> assertion) {
        expectedComponents.put(compKey, assertion);
    }

    protected void expectFileComponent(String distributionPath, Consumer<ApplicationComponent> assertion) {
        expectComponent(distributionPath, assertDistributionPath(distributionPath).andThen(assertion));
    }

    protected void expectMavenComponent(ArtifactCoords coords, Consumer<ApplicationComponent> assertion) {
        expectComponent(coords.toCompactCoords(), assertMavenComponent(coords).andThen(assertion));
    }

    private Consumer<ApplicationComponent> assertMavenComponent(ArtifactCoords expectedCoords) {
        return comp -> assertMavenComponent(comp, expectedCoords);
    }

    protected static void assertMavenComponent(ApplicationComponent comp, ArtifactCoords expectedCoords) {
        assertThat(comp.getResolvedDependency()).isNotNull();
        assertThat(comp.getResolvedDependency().toCompactCoords()).isEqualTo(expectedCoords.toCompactCoords());
    }

    protected static void assertDependencyScope(ApplicationComponent comp, String expectedScope) {
        assertThat(comp.getScope())
                .as(() -> ApplicationManifestTestBase.getComponentKey(comp) + " has scope")
                .isEqualTo(expectedScope);
    }

    protected Consumer<ApplicationComponent> assertDistributionPath(String expectedDistributionPath) {
        return comp -> assertDistributionPath(comp, expectedDistributionPath);
    }

    protected static void assertDistributionPath(ApplicationComponent comp, String expectedDistributionPath) {
        assertThat(comp.getDistributionPath())
                .as(() -> ApplicationManifestTestBase.getComponentKey(comp) + " has distribution path")
                .isEqualTo(expectedDistributionPath);
    }

    protected static void assertNoDistributionPath(ApplicationComponent comp) {
        assertThat(comp.getDistributionPath())
                .as(() -> ApplicationManifestTestBase.getComponentKey(comp) + " is not found in the distribution").isNull();
    }

    protected static void assertDependencies(ApplicationComponent comp, ArtifactCoords... expectedDeps) {
        assertThat(toArtifactCoordsList(comp.getDependencies()))
                .as(() -> ApplicationManifestTestBase.getComponentKey(comp) + " has dependencies")
                .containsExactlyInAnyOrder(expectedDeps);
    }

    /**
     * Makes sure the collection of {@link ArtifactCoords}'s uses the default implementation.}
     *
     * @param original original collection of artifact coordinates
     * @return a copy of the original collection using the default implementation
     */
    private static Collection<ArtifactCoords> toArtifactCoordsList(Collection<ArtifactCoords> original) {
        var result = new ArrayList<ArtifactCoords>(original.size());
        for (var a : original) {
            result.add(ArtifactCoords.of(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getType(), a.getVersion()));
        }
        return result;
    }

    @Override
    protected Class<?>[] targetBuildItems() {
        return new Class[] { ApplicationManifestsBuildItem.class };
    }

    @Override
    protected void assertBuildResult(BuildResult result) {
        var manifestsBuildItem = result.consume(ApplicationManifestsBuildItem.class);
        for (var manifest : manifestsBuildItem.getManifests()) {
            List<String> extraComponents = null;
            var assertion = expectedComponents.remove(ApplicationManifestTestBase.getComponentKey(manifest.getMainComponent()));
            if (assertion == null) {
                extraComponents = new ArrayList<>();
                extraComponents.add(ApplicationManifestTestBase.getComponentKey(manifest.getMainComponent()));
            } else {
                assertion.accept(manifest.getMainComponent());
            }
            for (var c : manifest.getComponents()) {
                assertion = expectedComponents.remove(ApplicationManifestTestBase.getComponentKey(c));
                if (assertion == null) {
                    if (extraComponents == null) {
                        extraComponents = new ArrayList<>();
                    }
                    extraComponents.add(ApplicationManifestTestBase.getComponentKey(c));
                } else {
                    assertion.accept(c);
                }
            }
            if (extraComponents != null) {
                var sb = new StringBuilder();
                sb.append("Unexpected components:").append(System.lineSeparator());
                for (var key : extraComponents) {
                    sb.append("- ").append(key).append(System.lineSeparator());
                }
                fail(sb.toString());
            }
            assertThat(extraComponents).isNull();
            if (!expectedComponents.isEmpty()) {
                assertThat(expectedComponents.keySet()).isEmpty();
            }
        }
    }
}
