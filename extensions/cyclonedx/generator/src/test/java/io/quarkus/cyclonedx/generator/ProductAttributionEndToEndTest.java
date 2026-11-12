package io.quarkus.cyclonedx.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.BomReference;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.parsers.JsonParser;
import org.cyclonedx.parsers.XmlParser;
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

    private static final String PLATFORM_PROPS_PREFIX = "platform.com.redhat.quarkus.platform.quarkus-camel-bom.";
    private static final String CPE = "cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*";

    private static final String CAMEL_RT_REF = Purl.maven("org.acme.camel", "camel-quarkus-core", "3.20.0", "jar", null)
            .toString();
    private static final String CAMEL_DEP_REF = Purl.maven("org.acme.camel", "camel-support", "3.20.0", "jar", null)
            .toString();

    @Test
    void appRootDependsOnProductWhichDependsOnAttributedArtifacts() throws Exception {
        Bom bom = generateBom("json", Map.of());

        Component product = component(bom, "quarkus-camel-bom");
        assertThat(product.getType()).isEqualTo(Component.Type.FRAMEWORK);
        assertThat(product.getScope()).isEqualTo(Component.Scope.EXCLUDED);
        assertThat(product.getCpe()).isEqualTo(CPE);
        assertThat(product.getVersion()).isEqualTo("3.20.0");

        assertProductRelationships(bom, product.getBomRef());
    }

    @Test
    void customProductNameAndVersion() throws Exception {
        Bom bom = generateBom("json", Map.of(
                "product-name", "Camel Extensions for Quarkus",
                "product-version", "3.20"));

        Component product = component(bom, "Camel Extensions for Quarkus");
        assertThat(product.getType()).isEqualTo(Component.Type.FRAMEWORK);
        assertThat(product.getScope()).isEqualTo(Component.Scope.EXCLUDED);
        assertThat(product.getCpe()).isEqualTo(CPE);
        assertThat(product.getVersion()).isEqualTo("3.20");

        assertProductRelationships(bom, product.getBomRef());
    }

    /**
     * {@code provides} relationships are represented in the XML format as well, not only in JSON.
     */
    @Test
    void providesIsEmittedInXmlFormat() throws Exception {
        Bom bom = generateBom("xml", Map.of());
        assertProductRelationships(bom, component(bom, "quarkus-camel-bom").getBomRef());
    }

    /**
     * Asserts that the application root depends on the product component, while the product itself
     * provides (rather than depends on) the artifacts attributed to it.
     */
    private static void assertProductRelationships(Bom bom, String productRef) {
        String mainRef = bom.getMetadata().getComponent().getBomRef();
        assertThat(dependsOn(bom, mainRef)).contains(productRef);
        assertThat(provides(bom, productRef)).contains(CAMEL_RT_REF, CAMEL_DEP_REF);
        assertThat(dependsOn(bom, productRef)).isEmpty();
    }

    private static Bom generateBom(String format, Map<String, String> extraPlatformProps) throws Exception {
        String content = CycloneDxSbomGenerator.newInstance()
                .setSchemaVersion("1.6")
                .setFormat(format)
                .setContributions(List.of(newCamelProductContribution(extraPlatformProps)))
                .generateText().get(0);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return "xml".equals(format) ? new XmlParser().parse(bytes) : new JsonParser().parse(bytes);
    }

    /**
     * Builds an SBOM contribution for an application importing a platform member BOM that declares a
     * product identity (a CPE) and attributes two Camel artifacts to it.
     *
     * @param extraPlatformProps additional platform properties (without the platform key prefix)
     * @return the SBOM contribution
     */
    private static SbomContribution newCamelProductContribution(Map<String, String> extraPlatformProps) {
        ArtifactCoords camelRt = ArtifactCoords.jar("org.acme.camel", "camel-quarkus-core", "3.20.0");
        ArtifactCoords camelDep = ArtifactCoords.jar("org.acme.camel", "camel-support", "3.20.0");

        Map<String, String> props = new HashMap<>();
        props.put(PLATFORM_PROPS_PREFIX + "cpe", CPE);
        props.put(PLATFORM_PROPS_PREFIX + "cpe-artifacts",
                CpeArtifactsEncoder.encode(Map.of(camelRt, List.of(camelRt, camelDep))));
        for (Map.Entry<String, String> prop : extraPlatformProps.entrySet()) {
            props.put(PLATFORM_PROPS_PREFIX + prop.getKey(), prop.getValue());
        }

        Map<String, Object> platformMap = new HashMap<>();
        platformMap.put(BootstrapConstants.MAPPABLE_PLATFORM_PROPS, props);
        platformMap.put(BootstrapConstants.MAPPABLE_IMPORTED_BOMS,
                List.of("com.redhat.quarkus.platform:quarkus-camel-bom::pom:3.20.0"));

        ApplicationModel model = new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme").setArtifactId("acme-app").setVersion("1.0.0")
                        .setResolvedPaths(PathList.of()).setRuntimeCp())
                .setPlatformImports(PlatformImports.fromMap(platformMap))
                .addDependency(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme.camel").setArtifactId("camel-quarkus-core").setVersion("3.20.0")
                        .setResolvedPaths(PathList.of()).setDependencies(List.of())
                        .setRuntimeCp().setDeploymentCp().setRuntimeExtensionArtifact())
                .addDependency(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme.camel").setArtifactId("camel-support").setVersion("3.20.0")
                        .setResolvedPaths(PathList.of()).setDependencies(List.of())
                        .setRuntimeCp().setDeploymentCp())
                .build();

        return new CoreSbomContributionConfig()
                .setApplicationModel(model)
                .toSbomContribution();
    }

    private static Component component(Bom bom, String name) {
        return bom.getComponents().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst().orElseThrow();
    }

    private static List<String> dependsOn(Bom bom, String ref) {
        Dependency dep = dependency(bom, ref);
        if (dep.getDependencies() == null) {
            return List.of();
        }
        List<String> refs = new ArrayList<>(dep.getDependencies().size());
        for (Dependency d : dep.getDependencies()) {
            refs.add(d.getRef());
        }
        return refs;
    }

    private static List<String> provides(Bom bom, String ref) {
        Dependency dep = dependency(bom, ref);
        if (dep.getProvides() == null) {
            return List.of();
        }
        List<String> refs = new ArrayList<>(dep.getProvides().size());
        for (BomReference provided : dep.getProvides()) {
            refs.add(provided.getRef());
        }
        return refs;
    }

    private static Dependency dependency(Bom bom, String ref) {
        for (Dependency dep : bom.getDependencies()) {
            if (ref.equals(dep.getRef())) {
                return dep;
            }
        }
        throw new AssertionError("no dependency entry for " + ref);
    }
}
