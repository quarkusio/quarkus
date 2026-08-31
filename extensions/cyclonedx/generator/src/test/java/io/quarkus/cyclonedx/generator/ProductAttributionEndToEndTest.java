package io.quarkus.cyclonedx.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.parsers.JsonParser;
import org.junit.jupiter.api.Test;

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

class ProductAttributionEndToEndTest {

    @Test
    void appRootDependsOnProductWhichDependsOnAttributedArtifacts() throws Exception {
        ArtifactCoords camelRt = ArtifactCoords.jar("org.acme.camel", "camel-quarkus-core", "3.20.0");
        ArtifactCoords camelDep = ArtifactCoords.jar("org.acme.camel", "camel-support", "3.20.0");
        String cpeArtifacts = CpeArtifactsEncoder.encode(Map.of(camelRt, List.of(camelDep)));

        Map<String, String> props = new HashMap<>();
        String prefix = "platform.com.redhat.quarkus.platform.quarkus-camel-bom.";
        props.put(prefix + "cpe", "cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*");
        //props.put(prefix + "product-name", "Camel Extensions for Quarkus");
        //props.put(prefix + "product-version", "3.20");
        //props.put(prefix + "product-type", "framework");
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

        List<String> result = CycloneDxSbomGenerator.newInstance()
                .setSchemaVersion("1.6")
                .setContributions(List.of(contribution))
                .generateText();

        Bom bom = new JsonParser().parse(result.get(0).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // product component
        Component product = bom.getComponents().stream()
                .filter(c -> "quarkus-camel-bom".equals(c.getName()))
                .findFirst().orElseThrow();
        String productRef = product.getBomRef();
        assertThat(product.getType()).isEqualTo(Component.Type.FRAMEWORK);
        assertThat(product.getScope()).isEqualTo(Component.Scope.EXCLUDED);
        assertThat(product.getCpe()).isEqualTo("cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*");
        assertThat(product.getVersion()).isEqualTo("3.20.0");

        String mainRef = bom.getMetadata().getComponent().getBomRef();
        String camelRtRef = Purl.maven("org.acme.camel", "camel-quarkus-core", "3.20.0", "jar", null).toString();
        String camelDepRef = Purl.maven("org.acme.camel", "camel-support", "3.20.0", "jar", null).toString();

        // app root dependsOn product
        assertThat(dependsOn(bom, mainRef)).contains(productRef);
        // product dependsOn attributed artifacts
        assertThat(dependsOn(bom, productRef)).contains(camelRtRef, camelDepRef);
    }

    @Test
    void customProductNameAndVersion() throws Exception {
        ArtifactCoords camelRt = ArtifactCoords.jar("org.acme.camel", "camel-quarkus-core", "3.20.0");
        ArtifactCoords camelDep = ArtifactCoords.jar("org.acme.camel", "camel-support", "3.20.0");
        String cpeArtifacts = CpeArtifactsEncoder.encode(Map.of(camelRt, List.of(camelDep)));

        Map<String, String> props = new HashMap<>();
        String prefix = "platform.com.redhat.quarkus.platform.quarkus-camel-bom.";
        props.put(prefix + "cpe", "cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*");
        props.put(prefix + "product-name", "Camel Extensions for Quarkus");
        props.put(prefix + "product-version", "3.20");
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

        List<String> result = CycloneDxSbomGenerator.newInstance()
                .setSchemaVersion("1.6")
                .setContributions(List.of(contribution))
                .generateText();

        Bom bom = new JsonParser().parse(result.get(0).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // product component
        Component product = bom.getComponents().stream()
                .filter(c -> "Camel Extensions for Quarkus".equals(c.getName()))
                .findFirst().orElseThrow();
        String productRef = product.getBomRef();
        assertThat(product.getType()).isEqualTo(Component.Type.FRAMEWORK);
        assertThat(product.getScope()).isEqualTo(Component.Scope.EXCLUDED);
        assertThat(product.getCpe()).isEqualTo("cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*");
        assertThat(product.getVersion()).isEqualTo("3.20");

        String mainRef = bom.getMetadata().getComponent().getBomRef();
        String camelRtRef = Purl.maven("org.acme.camel", "camel-quarkus-core", "3.20.0", "jar", null).toString();
        String camelDepRef = Purl.maven("org.acme.camel", "camel-support", "3.20.0", "jar", null).toString();

        // app root dependsOn product
        assertThat(dependsOn(bom, mainRef)).contains(productRef);
        // product dependsOn attributed artifacts
        assertThat(dependsOn(bom, productRef)).contains(camelRtRef, camelDepRef);
    }

    private static List<String> dependsOn(Bom bom, String ref) {
        Dependency d = bom.getDependencies().stream()
                .filter(x -> ref.equals(x.getRef()))
                .findFirst().orElseThrow(() -> new AssertionError("no dependency entry for " + ref));
        return d.getDependencies() == null ? List.of()
                : d.getDependencies().stream().map(Dependency::getRef).toList();
    }
}
