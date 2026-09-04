package io.quarkus.cyclonedx.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.parsers.JsonParser;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;
import io.quarkus.sbom.CoreSbomContributionConfig;
import io.quarkus.sbom.CpeArtifactsEncoder;
import io.quarkus.sbom.Purl;
import io.quarkus.sbom.SbomContribution;

class DependencySortingTest {

    /**
     * A dependency node's relationship refs are sorted from the merged collection, not per input
     * batch, so the emitted order must be sorted regardless of the order in which the refs were
     * provided. Here the product's attributed artifacts are supplied in reverse-sorted order.
     */
    @Test
    void relationshipRefsAreEmittedSorted() throws Exception {
        ArtifactCoords camelRt = ArtifactCoords.jar("org.acme.camel", "camel-quarkus-core", "3.20.0");
        ArtifactCoords camelDep = ArtifactCoords.jar("org.acme.camel", "camel-support", "3.20.0");
        // supplied in reverse-sorted order (camel-support before camel-quarkus-core)
        String cpeArtifacts = CpeArtifactsEncoder.encode(Map.of(camelRt, List.of(camelDep, camelRt)));

        Map<String, String> props = new HashMap<>();
        String prefix = "platform.com.redhat.quarkus.platform.quarkus-camel-bom.";
        props.put(prefix + "cpe", "cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*");
        props.put(prefix + "cpe-artifacts", cpeArtifacts);

        Map<String, Object> platformMap = new HashMap<>();
        platformMap.put(BootstrapConstants.MAPPABLE_PLATFORM_PROPS, props);
        platformMap.put(BootstrapConstants.MAPPABLE_IMPORTED_BOMS,
                List.of("com.redhat.quarkus.platform:quarkus-camel-bom::pom:3.20.0"));
        PlatformImports imports = PlatformImports.fromMap(platformMap);

        ApplicationModel model = new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme").setArtifactId("acme-app").setVersion("1.0.0")
                        .setResolvedPaths(PathList.of()).setRuntimeCp())
                .setPlatformImports(imports)
                .addDependency(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme.camel").setArtifactId("camel-quarkus-core").setVersion("3.20.0")
                        .setResolvedPaths(PathList.of()).setDependencies(List.of())
                        .setRuntimeCp().setDeploymentCp().setRuntimeExtensionArtifact())
                .addDependency(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme.camel").setArtifactId("camel-support").setVersion("3.20.0")
                        .setResolvedPaths(PathList.of()).setDependencies(List.of())
                        .setRuntimeCp().setDeploymentCp())
                .build();

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setApplicationModel(model)
                .toSbomContribution();

        String json = CycloneDxSbomGenerator.newInstance()
                .setSchemaVersion("1.6")
                .setContributions(List.of(contribution))
                .generateText().get(0);

        Bom bom = new JsonParser().parse(json.getBytes(StandardCharsets.UTF_8));
        Component product = bom.getComponents().stream()
                .filter(c -> "quarkus-camel-bom".equals(c.getName()))
                .findFirst().orElseThrow();

        String camelRtRef = Purl.maven("org.acme.camel", "camel-quarkus-core", "3.20.0", "jar", null).toString();
        String camelDepRef = Purl.maven("org.acme.camel", "camel-support", "3.20.0", "jar", null).toString();

        List<String> provides = provides(json, product.getBomRef());
        // emitted in sorted order even though the artifacts were supplied reversed
        assertThat(provides).containsExactly(camelRtRef, camelDepRef);
        assertThat(provides).isSorted();
    }

    private static List<String> provides(String json, String ref) {
        try {
            JsonNode dependencies = new ObjectMapper().readTree(json).get("dependencies");
            for (JsonNode dep : dependencies) {
                if (ref.equals(dep.path("ref").asText())) {
                    JsonNode array = dep.get("provides");
                    if (array == null) {
                        return List.of();
                    }
                    List<String> refs = new ArrayList<>(array.size());
                    array.forEach(n -> refs.add(n.asText()));
                    return refs;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SBOM JSON", e);
        }
        throw new AssertionError("no dependency entry for " + ref);
    }
}
