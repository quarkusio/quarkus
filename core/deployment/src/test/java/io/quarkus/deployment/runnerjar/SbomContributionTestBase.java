package io.quarkus.deployment.runnerjar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.sbom.ComponentDependencies;
import io.quarkus.sbom.ComponentDescriptor;
import io.quarkus.sbom.CoreSbomContributionConfig;
import io.quarkus.sbom.Purl;
import io.quarkus.sbom.SbomContribution;

public abstract class SbomContributionTestBase extends BootstrapFromOriginalJarTestBase {

    private static String getComponentKey(ComponentDescriptor desc) {
        return Purl.TYPE_MAVEN.equals(desc.getPurl().getType())
                ? desc.getBomRef()
                : desc.getDistributionPath();
    }

    protected static ArtifactCoords artifactCoords(String artifactId) {
        return ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, artifactId, TsArtifact.DEFAULT_VERSION);
    }

    protected static ArtifactCoords artifactCoords(String artifactId, String classifier) {
        return ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, artifactId, classifier, TsArtifact.DEFAULT_VERSION);
    }

    private final Map<String, Consumer<ComponentDescriptor>> expectedComponents = new HashMap<>();
    private Map<String, ComponentDependencies> dependenciesMap;

    private void expectComponent(String compKey, Consumer<ComponentDescriptor> assertion) {
        expectedComponents.put(compKey, assertion);
    }

    protected void expectFileComponent(String distributionPath, Consumer<ComponentDescriptor> assertion) {
        expectComponent(distributionPath, desc -> {
            assertThat(desc.getDistributionPath()).isEqualTo(distributionPath);
            assertion.accept(desc);
        });
    }

    protected void expectMavenComponent(ArtifactCoords coords, Consumer<ComponentDescriptor> assertion) {
        String bomRef = toBomRef(coords);
        expectComponent(bomRef, desc -> {
            assertMavenComponent(desc, coords);
            assertion.accept(desc);
        });
    }

    private static String toBomRef(ArtifactCoords coords) {
        return Purl.maven(coords.getGroupId(), coords.getArtifactId(), coords.getVersion(),
                coords.getType(), coords.getClassifier().isEmpty() ? null : coords.getClassifier()).toString();
    }

    protected static void assertMavenComponent(ComponentDescriptor desc, ArtifactCoords expectedCoords) {
        Purl purl = desc.getPurl();
        assertThat(purl.getType()).isEqualTo(Purl.TYPE_MAVEN);
        assertThat(purl.getNamespace()).isEqualTo(expectedCoords.getGroupId());
        assertThat(purl.getName()).isEqualTo(expectedCoords.getArtifactId());
        assertThat(purl.getVersion()).isEqualTo(expectedCoords.getVersion());
        assertThat(purl.getQualifiers().get("type")).isEqualTo(expectedCoords.getType());
        String expectedClassifier = expectedCoords.getClassifier().isEmpty() ? null
                : expectedCoords.getClassifier();
        assertThat(purl.getQualifiers().get("classifier")).isEqualTo(expectedClassifier);
    }

    protected static void assertDependencyScope(ComponentDescriptor desc, String expectedScope) {
        assertThat(desc.getScope())
                .as(() -> desc.getBomRef() + " has scope")
                .isEqualTo(expectedScope);
    }

    protected void assertDistributionPath(ComponentDescriptor desc, String expectedDistributionPath) {
        assertThat(desc.getDistributionPath())
                .as(() -> desc.getBomRef() + " has distribution path")
                .isEqualTo(expectedDistributionPath);
    }

    protected static void assertNoDistributionPath(ComponentDescriptor desc) {
        assertThat(desc.getDistributionPath())
                .as(() -> desc.getBomRef() + " is not found in the distribution").isNull();
    }

    protected static void assertVersion(ComponentDescriptor desc, String expectedVersion) {
        assertThat(desc.getVersion())
                .as(() -> desc.getBomRef() + " has version")
                .isEqualTo(expectedVersion);
    }

    protected void assertDependencies(ComponentDescriptor desc, ArtifactCoords... expectedDeps) {
        ComponentDependencies deps = dependenciesMap.get(desc.getBomRef());
        if (expectedDeps.length == 0) {
            assertThat(deps == null || deps.getDependsOn().isEmpty())
                    .as(() -> desc.getBomRef() + " has no dependencies")
                    .isTrue();
        } else {
            assertThat(deps)
                    .as(() -> desc.getBomRef() + " has dependency record")
                    .isNotNull();
            Set<String> expectedBomRefs = new HashSet<String>(expectedDeps.length);
            for (ArtifactCoords coords : expectedDeps) {
                expectedBomRefs.add(toBomRef(coords));
            }
            assertThat(new HashSet<>(deps.getDependsOn()))
                    .as(() -> desc.getBomRef() + " has dependencies")
                    .isEqualTo(expectedBomRefs);
        }
    }

    @Override
    protected Class<?>[] targetBuildItems() {
        return new Class[] { ArtifactResultBuildItem.class };
    }

    @Override
    protected void assertBuildResult(BuildResult result) {
        List<ArtifactResultBuildItem> artifactResults = result.consumeMulti(ArtifactResultBuildItem.class);

        // Build core SBOM contributions from artifact results and collect all descriptors
        List<ComponentDescriptor> allDescriptors = new ArrayList<>();
        dependenciesMap = new HashMap<>();
        for (ArtifactResultBuildItem artifactResult : artifactResults) {
            CoreSbomContributionConfig manifestConfig = artifactResult.getCoreSbomConfig();
            if (manifestConfig != null) {
                SbomContribution contribution = manifestConfig.toSbomContribution();
                allDescriptors.addAll(contribution.components());
                for (ComponentDependencies dep : contribution.dependencies()) {
                    dependenciesMap.put(dep.getBomRef(), dep);
                }
            }
        }

        List<String> extraComponents = null;
        for (ComponentDescriptor descriptor : allDescriptors) {
            String key = getComponentKey(descriptor);
            Consumer<ComponentDescriptor> assertion = expectedComponents.remove(key);
            if (assertion == null) {
                if (extraComponents == null) {
                    extraComponents = new ArrayList<>();
                }
                extraComponents.add(key);
            } else {
                assertion.accept(descriptor);
            }
        }
        if (extraComponents != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unexpected components:").append(System.lineSeparator());
            for (String key : extraComponents) {
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
