package io.quarkus.sbom;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;

class CoreSbomContributionConfigTest {

    @Test
    void directDependenciesAreMarkedTopLevel() {
        ResolvedDependency directDep = resolvedDep("io.quarkus", "quarkus-rest", "3.0.0", List.of());
        ResolvedDependency transitiveDep = resolvedDep("io.quarkus", "quarkus-vertx", "3.0.0", List.of());
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0",
                List.of(ArtifactCoords.jar("io.quarkus", "quarkus-rest", "3.0.0")));

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .addComponent(directDep)
                .addComponent(transitiveDep)
                .toSbomContribution();

        ComponentDescriptor direct = findByName(contribution, "quarkus-rest");
        assertThat(direct.isTopLevel())
                .as("Direct dependency should be top-level")
                .isTrue();

        ComponentDescriptor transitive = findByName(contribution, "quarkus-vertx");
        assertThat(transitive.isTopLevel())
                .as("Transitive dependency should not be top-level")
                .isFalse();
    }

    @Test
    void mainComponentIsNotMarkedTopLevel() {
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .toSbomContribution();

        ComponentDescriptor mainComp = contribution.components().stream()
                .filter(c -> c.getBomRef().equals(contribution.mainComponentBomRef()))
                .findFirst()
                .orElseThrow();
        assertThat(mainComp.isTopLevel())
                .as("Main component itself should not be top-level")
                .isFalse();
    }

    @Test
    void explicitDependenciesMarkedTopLevel() {
        ResolvedDependency directDep = resolvedDep("io.quarkus", "quarkus-rest", "3.0.0", List.of());
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .setMainDependencies(List.of(ArtifactCoords.jar("io.quarkus", "quarkus-rest", "3.0.0")))
                .addComponent(directDep)
                .toSbomContribution();

        ComponentDescriptor direct = findByName(contribution, "quarkus-rest");
        assertThat(direct.isTopLevel())
                .as("Component matching explicit dependency should be top-level")
                .isTrue();
    }

    @Test
    void shadedJarBundledComponentsDetected(@TempDir Path tempDir) throws IOException {
        Path shadedJar = createShadedJar(tempDir,
                "com.example", "shaded-lib", "1.0.0",
                "org.bundled", "bundled-dep", "2.0.0");

        ResolvedDependency dep = ResolvedDependencyBuilder.newInstance()
                .setGroupId("com.example")
                .setArtifactId("shaded-lib")
                .setVersion("1.0.0")
                .setResolvedPaths(PathList.of(shadedJar))
                .setDependencies(List.of())
                .setRuntimeCp()
                .build();
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .addComponent(dep)
                .toSbomContribution();

        ComponentDescriptor parent = findByName(contribution, "shaded-lib");
        assertThat(parent.getComponents())
                .as("Shaded JAR should have nested bundled components")
                .hasSize(1);

        ComponentDescriptor nested = parent.getComponents().get(0);
        assertThat(nested.getName()).isEqualTo("bundled-dep");
        assertThat(nested.getNamespace()).isEqualTo("org.bundled");
        assertThat(nested.getVersion()).isEqualTo("2.0.0");
        assertThat(nested.getBomRef())
                .isEqualTo(Purl.maven("org.bundled", "bundled-dep", "2.0.0", "jar", null) + "#bundled");
    }

    @Test
    void shadedJarBomRefUniqueFromStandaloneDependency(@TempDir Path tempDir) throws IOException {
        Path shadedJar = createShadedJar(tempDir,
                "com.example", "shaded-lib", "1.0.0",
                "org.bundled", "bundled-dep", "2.0.0");

        ResolvedDependency shadedDep = ResolvedDependencyBuilder.newInstance()
                .setGroupId("com.example")
                .setArtifactId("shaded-lib")
                .setVersion("1.0.0")
                .setResolvedPaths(PathList.of(shadedJar))
                .setDependencies(List.of())
                .setRuntimeCp()
                .build();
        ResolvedDependency standaloneDep = resolvedDep("org.bundled", "bundled-dep", "2.0.0", List.of());
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .addComponent(standaloneDep)
                .addComponent(shadedDep)
                .toSbomContribution();

        ComponentDescriptor standalone = findByName(contribution, "bundled-dep");
        ComponentDescriptor parent = findByName(contribution, "shaded-lib");
        ComponentDescriptor nested = parent.getComponents().get(0);

        assertThat(standalone.getBomRef())
                .as("Standalone gets the plain PURL bomRef")
                .doesNotContain("#");
        assertThat(nested.getBomRef())
                .as("Nested bomRef gets #bundled suffix")
                .endsWith("#bundled");
        assertThat(nested.getBomRef())
                .as("Nested and standalone bomRefs must differ")
                .isNotEqualTo(standalone.getBomRef());
    }

    @Test
    void nonShadedJarHasNoNestedComponents(@TempDir Path tempDir) throws IOException {
        Path normalJar = createShadedJar(tempDir,
                "com.example", "normal-lib", "1.0.0");

        ResolvedDependency dep = ResolvedDependencyBuilder.newInstance()
                .setGroupId("com.example")
                .setArtifactId("normal-lib")
                .setVersion("1.0.0")
                .setResolvedPaths(PathList.of(normalJar))
                .setDependencies(List.of())
                .setRuntimeCp()
                .build();
        ResolvedDependency mainArtifact = resolvedDep("org.acme", "acme-app", "1.0.0", List.of());

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setMainArtifact(mainArtifact)
                .addComponent(dep)
                .toSbomContribution();

        ComponentDescriptor parent = findByName(contribution, "normal-lib");
        assertThat(parent.getComponents()).isEmpty();
    }

    private static Path createShadedJar(Path dir, String ownerGroupId, String ownerArtifactId, String ownerVersion,
            String... shadedGavTriples) throws IOException {
        Path jar = dir.resolve(ownerArtifactId + "-" + ownerVersion + ".jar");
        try (OutputStream fos = Files.newOutputStream(jar);
                ZipOutputStream zos = new ZipOutputStream(fos)) {
            writePomProperties(zos, ownerGroupId, ownerArtifactId, ownerVersion);
            for (int i = 0; i < shadedGavTriples.length; i += 3) {
                writePomProperties(zos, shadedGavTriples[i], shadedGavTriples[i + 1], shadedGavTriples[i + 2]);
            }
        }
        return jar;
    }

    private static void writePomProperties(ZipOutputStream zos, String groupId, String artifactId, String version)
            throws IOException {
        String path = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        zos.putNextEntry(new ZipEntry(path));
        Properties props = new Properties();
        props.setProperty("groupId", groupId);
        props.setProperty("artifactId", artifactId);
        props.setProperty("version", version);
        props.store(zos, null);
        zos.closeEntry();
    }

    private static ComponentDescriptor findByName(SbomContribution contribution, String name) {
        return contribution.components().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(name + " not found"));
    }

    private static io.quarkus.maven.dependency.ResolvedDependency resolvedDep(
            String groupId, String artifactId, String version,
            List<ArtifactCoords> dependencies) {
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId(groupId)
                .setArtifactId(artifactId)
                .setVersion(version)
                .setResolvedPaths(PathList.of())
                .setDependencies(dependencies)
                .setRuntimeCp()
                .build();
    }

    @Test
    void memberProductComponentAttributesUsedRuntimeExtensions() {
        // Used runtime extension belonging to the Camel member, plus its attributed dep.
        ArtifactCoords camelRt = ArtifactCoords.jar("org.acme.camel", "camel-quarkus-core", "3.20.0");
        ArtifactCoords camelDep = ArtifactCoords.jar("org.acme.camel", "camel-support", "3.20.0");
        // An unrelated runtime extension not covered by the member.
        ArtifactCoords otherRt = ArtifactCoords.jar("io.quarkus", "quarkus-rest", "3.20.0");

        // By convention the deployment closure contains the runtime artifact, so the key appears in its value.
        String cpeArtifacts = CpeArtifactsEncoder.encode(Map.of(camelRt, List.of(camelRt, camelDep)));

        Map<String, String> props = new HashMap<>();
        String prefix = "platform.com.redhat.quarkus.platform.quarkus-camel-bom.";
        props.put(prefix + "cpe", "cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*");
        props.put(prefix + "product-name", "Camel Extensions for Quarkus");
        props.put(prefix + "product-version", "3.20");
        props.put(prefix + "product-type", "framework");
        props.put(prefix + "cpe-artifacts", cpeArtifacts);

        ApplicationModel model = buildModel(
                props,
                List.of("com.redhat.quarkus.platform:quarkus-camel-bom::pom:3.20.0"),
                camelRt, camelDep, otherRt);

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setApplicationModel(model)
                .toSbomContribution();

        // Product component present, framework type carried via componentType, excluded scope, topLevel.
        ComponentDescriptor product = contribution.components().stream()
                .filter(c -> "Camel Extensions for Quarkus".equals(c.getName()))
                .findFirst().orElseThrow();
        assertThat(product.getCpe()).isEqualTo("cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*");
        assertThat(product.getComponentType()).isEqualTo("framework");
        assertThat(product.getScope()).isEqualTo(ComponentDescriptor.SCOPE_EXCLUDED);
        assertThat(product.isTopLevel()).isTrue();

        // Product provides the used runtime extension and its attributed dep (both present as components),
        // but not the unrelated extension.
        String productRef = product.getBomRef();
        Collection<String> productProvides = contribution.dependencies().stream()
                .filter(d -> productRef.equals(d.getBomRef()))
                .findFirst().orElseThrow()
                .getProvides();
        assertThat(productProvides).contains(
                Purl.maven("org.acme.camel", "camel-quarkus-core", "3.20.0", "jar", null).toString(),
                Purl.maven("org.acme.camel", "camel-support", "3.20.0", "jar", null).toString());
        assertThat(productProvides).doesNotContain(
                Purl.maven("io.quarkus", "quarkus-rest", "3.20.0", "jar", null).toString());
    }

    @Test
    void productComponentEmittedWhenCpePresentButCpeArtifactsAbsent() {
        ArtifactCoords camelRt = ArtifactCoords.jar("org.acme.camel", "camel-quarkus-core", "3.20.0");

        Map<String, String> props = new HashMap<>();
        String prefix = "platform.com.redhat.quarkus.platform.quarkus-camel-bom.";
        props.put(prefix + "cpe", "cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*");
        props.put(prefix + "product-name", "Camel Extensions for Quarkus");

        ApplicationModel model = buildModel(
                props,
                List.of("com.redhat.quarkus.platform:quarkus-camel-bom::pom:3.20.0"),
                camelRt);

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setApplicationModel(model)
                .toSbomContribution();

        assertThat(contribution.components())
                .anyMatch(c -> "Camel Extensions for Quarkus".equals(c.getName()));
    }

    @Test
    void productAttributionDisabledEmitsNoProductComponent() {
        ArtifactCoords camelRt = ArtifactCoords.jar("org.acme.camel", "camel-quarkus-core", "3.20.0");
        Map<String, String> props = new HashMap<>();
        String prefix = "platform.com.redhat.quarkus.platform.quarkus-camel-bom.";
        props.put(prefix + "cpe", "cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*");
        props.put(prefix + "product-name", "Camel Extensions for Quarkus");

        ApplicationModel model = buildModel(
                props,
                List.of("com.redhat.quarkus.platform:quarkus-camel-bom::pom:3.20.0"),
                camelRt);

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setApplicationModel(model)
                .setProductAttribution(false)
                .toSbomContribution();

        assertThat(contribution.components())
                .noneMatch(c -> "Camel Extensions for Quarkus".equals(c.getName()));
    }

    @Test
    void membersSharingPurlAndCpeAreMergedIntoSingleProduct() {
        // Two members map to the same product (same product-purl and CPE) but contribute different extensions.
        ArtifactCoords rt1 = ArtifactCoords.jar("org.acme.camel", "camel-quarkus-core", "3.20.0");
        ArtifactCoords dep1 = ArtifactCoords.jar("org.acme.camel", "camel-core-support", "3.20.0");
        ArtifactCoords rt2 = ArtifactCoords.jar("org.acme.camel", "camel-quarkus-http", "3.20.0");
        ArtifactCoords dep2 = ArtifactCoords.jar("org.acme.camel", "camel-http-support", "3.20.0");

        String cpe = "cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*";
        String productPurl = Purl.maven("com.redhat", "camel-quarkus-product", "3.20", "pom", null).toString();

        Map<String, String> props = new HashMap<>();
        String prefix1 = "platform.com.redhat.quarkus.platform.quarkus-camel-bom.";
        String prefix2 = "platform.com.redhat.quarkus.platform.quarkus-camel-extra-bom.";
        for (String prefix : List.of(prefix1, prefix2)) {
            props.put(prefix + "cpe", cpe);
            props.put(prefix + "product-purl", productPurl);
            props.put(prefix + "product-name", "Camel Extensions for Quarkus");
        }
        props.put(prefix1 + "cpe-artifacts", CpeArtifactsEncoder.encode(Map.of(rt1, List.of(rt1, dep1))));
        props.put(prefix2 + "cpe-artifacts", CpeArtifactsEncoder.encode(Map.of(rt2, List.of(rt2, dep2))));

        ApplicationModel model = buildModel(
                props,
                List.of("com.redhat.quarkus.platform:quarkus-camel-bom::pom:3.20.0",
                        "com.redhat.quarkus.platform:quarkus-camel-extra-bom::pom:3.20.0"),
                rt1, dep1, rt2, dep2);

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setApplicationModel(model)
                .toSbomContribution();

        List<ComponentDescriptor> products = contribution.components().stream()
                .filter(c -> cpe.equals(c.getCpe()))
                .toList();
        assertThat(products).hasSize(1);

        ComponentDescriptor product = products.get(0);
        String productRef = product.getBomRef();
        Collection<String> productProvides = contribution.dependencies().stream()
                .filter(d -> productRef.equals(d.getBomRef()))
                .findFirst().orElseThrow()
                .getProvides();
        assertThat(productProvides).contains(
                Purl.maven("org.acme.camel", "camel-quarkus-core", "3.20.0", "jar", null).toString(),
                Purl.maven("org.acme.camel", "camel-core-support", "3.20.0", "jar", null).toString(),
                Purl.maven("org.acme.camel", "camel-quarkus-http", "3.20.0", "jar", null).toString(),
                Purl.maven("org.acme.camel", "camel-http-support", "3.20.0", "jar", null).toString());
    }

    @Test
    void membersSharingCpeButDifferentPurlStaySeparate() {
        ArtifactCoords rt1 = ArtifactCoords.jar("org.acme.camel", "camel-quarkus-core", "3.20.0");
        ArtifactCoords rt2 = ArtifactCoords.jar("org.acme.camel", "camel-quarkus-http", "3.20.0");

        String cpe = "cpe:2.3:a:redhat:camel_quarkus:3.20:*:*:*:*:*:*:*";

        Map<String, String> props = new HashMap<>();
        String prefix1 = "platform.com.redhat.quarkus.platform.quarkus-camel-bom.";
        String prefix2 = "platform.com.redhat.quarkus.platform.quarkus-camel-extra-bom.";
        // Same CPE, but no product-purl so each derives a distinct PURL from its own member BOM coordinates.
        props.put(prefix1 + "cpe", cpe);
        props.put(prefix1 + "product-name", "Camel Core");
        props.put(prefix1 + "cpe-artifacts", CpeArtifactsEncoder.encode(Map.of(rt1, List.of(rt1))));
        props.put(prefix2 + "cpe", cpe);
        props.put(prefix2 + "product-name", "Camel HTTP");
        props.put(prefix2 + "cpe-artifacts", CpeArtifactsEncoder.encode(Map.of(rt2, List.of(rt2))));

        ApplicationModel model = buildModel(
                props,
                List.of("com.redhat.quarkus.platform:quarkus-camel-bom::pom:3.20.0",
                        "com.redhat.quarkus.platform:quarkus-camel-extra-bom::pom:3.20.0"),
                rt1, rt2);

        SbomContribution contribution = new CoreSbomContributionConfig()
                .setApplicationModel(model)
                .toSbomContribution();

        List<ComponentDescriptor> products = contribution.components().stream()
                .filter(c -> cpe.equals(c.getCpe()))
                .toList();
        assertThat(products).hasSize(2);
        assertThat(products).extracting(c -> c.getPurl().toString()).doesNotHaveDuplicates();
    }

    private static ApplicationModel buildModel(Map<String, String> platformProps,
            List<String> importedBomGactvs, ArtifactCoords... runtimeExtensions) {
        Map<String, Object> platformMap = new HashMap<>();
        platformMap.put(BootstrapConstants.MAPPABLE_PLATFORM_PROPS, platformProps);
        platformMap.put(BootstrapConstants.MAPPABLE_IMPORTED_BOMS, importedBomGactvs);
        PlatformImports imports = PlatformImports.fromMap(platformMap);

        ApplicationModelBuilder builder = new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme")
                        .setArtifactId("acme-app")
                        .setVersion("1.0.0")
                        .setResolvedPaths(PathList.of())
                        .setRuntimeCp());
        builder.setPlatformImports(imports);
        for (ArtifactCoords rt : runtimeExtensions) {
            builder.addDependency(ResolvedDependencyBuilder.newInstance()
                    .setGroupId(rt.getGroupId())
                    .setArtifactId(rt.getArtifactId())
                    .setVersion(rt.getVersion())
                    .setResolvedPaths(PathList.of())
                    .setDependencies(List.of())
                    .setRuntimeCp()
                    .setDeploymentCp()
                    .setRuntimeExtensionArtifact());
        }
        return builder.build();
    }
}
