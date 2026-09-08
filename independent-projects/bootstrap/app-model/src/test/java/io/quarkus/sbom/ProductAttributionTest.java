package io.quarkus.sbom;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;

class ProductAttributionTest {

    private static final String CPE = "cpe:2.3:a:acme:acme_product:1.0:*:*:*:*:*:*:*";
    // by Quarkus convention a deployment artifact depends on its runtime artifact, so the runtime
    // extension key always appears in its own dependency closure
    private static final ArtifactCoords RT_EXT = ArtifactCoords.jar("org.acme", "acme-ext", "1.0");
    private static final ArtifactCoords DEPLOYMENT_DEP = ArtifactCoords.jar("org.acme", "acme-ext-deployment", "1.0");

    @Test
    void attributesPresentArtifactsToProduct() {
        final ApplicationModel model = model(
                Map.of(RT_EXT, List.of(RT_EXT, DEPLOYMENT_DEP)),
                List.of(runtimeExtension("acme-ext"), dependency("acme-ext-deployment")),
                Map.of());
        final ApplicationManifest augmented = ProductAttribution.augment(manifestOf(model), model);

        final ApplicationComponent product = productComponent(augmented);
        assertThat(product.getCpe()).isEqualTo(CPE);
        assertThat(product.getType()).isEqualTo(ApplicationComponent.TYPE_FRAMEWORK);
        assertThat(product.getScope()).isEqualTo(ApplicationComponent.SCOPE_EXCLUDED);
        assertThat(product.getCoordinates()).isEqualTo(ArtifactCoords.pom("org.acme", "acme-bom", "1.0"));
        assertThat(product.getProvides()).containsExactlyInAnyOrder(RT_EXT, DEPLOYMENT_DEP);

        // the product is linked as a dependency of the main application component
        assertThat(augmented.getMainComponent().getDependencies())
                .contains(ArtifactCoords.pom("org.acme", "acme-bom", "1.0"));
    }

    @Test
    void productComponentEmittedEvenWithoutPresentAttributedArtifacts() {
        // the cpe-artifacts describe an extension the application does not use
        final ArtifactCoords unusedExt = ArtifactCoords.jar("org.acme", "unused-ext", "1.0");
        final ApplicationModel model = model(
                Map.of(unusedExt, List.of(unusedExt)),
                List.of(runtimeExtension("acme-ext")),
                Map.of());
        final ApplicationManifest augmented = ProductAttribution.augment(manifestOf(model), model);

        final ApplicationComponent product = productComponent(augmented);
        assertThat(product.getCpe()).isEqualTo(CPE);
        assertThat(product.getProvides()).isEmpty();
    }

    @Test
    void customProductNameVersionAndType() {
        final ApplicationModel model = model(
                Map.of(RT_EXT, List.of(RT_EXT, DEPLOYMENT_DEP)),
                List.of(runtimeExtension("acme-ext"), dependency("acme-ext-deployment")),
                Map.of("product-name", "Acme Product",
                        "product-version", "2.0",
                        "product-type", "application"));
        final ApplicationManifest augmented = ProductAttribution.augment(manifestOf(model), model);

        final ApplicationComponent product = productComponent(augmented);
        assertThat(product.getName()).isEqualTo("Acme Product");
        assertThat(product.getType()).isEqualTo("application");
        // product-version overrides the member BOM version in the product coordinates
        assertThat(product.getCoordinates()).isEqualTo(ArtifactCoords.pom("org.acme", "acme-bom", "2.0"));
    }

    @Test
    void noAttributionWithoutPlatformProperties() {
        final ApplicationModel model = new ApplicationModelBuilder()
                .setAppArtifact(app())
                .addDependency(runtimeExtension("acme-ext"))
                .build();
        final ApplicationManifest manifest = manifestOf(model);
        assertThat(ProductAttribution.augment(manifest, model)).isSameAs(manifest);
    }

    private static ApplicationComponent productComponent(ApplicationManifest manifest) {
        return manifest.getComponents().stream()
                .filter(c -> c.getCpe() != null)
                .findFirst().orElseThrow(() -> new AssertionError("no product component found"));
    }

    private static ApplicationManifest manifestOf(ApplicationModel model) {
        return ApplicationManifest.fromConfig(ApplicationManifestConfig.builder()
                .setApplicationModel(model)
                .build());
    }

    private static ApplicationModel model(Map<ArtifactCoords, List<ArtifactCoords>> cpeArtifacts,
            List<ResolvedDependencyBuilder> deps, Map<String, String> extraProductProps) {
        final Map<String, String> props = new HashMap<>();
        final String prefix = "platform.org.acme.acme-bom.";
        props.put(prefix + "cpe", CPE);
        props.put(prefix + "cpe-artifacts", CpeArtifactsEncoder.encode(cpeArtifacts));
        extraProductProps.forEach((k, v) -> props.put(prefix + k, v));

        final PlatformImportsImpl platformImports = new PlatformImportsImpl();
        platformImports.addPlatformDescriptor("org.acme",
                "acme-bom" + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX, null, "json", "1.0");
        platformImports.setPlatformProperties(props);

        final ApplicationModelBuilder builder = new ApplicationModelBuilder()
                .setAppArtifact(app())
                .setPlatformImports(platformImports);
        for (ResolvedDependencyBuilder dep : new ArrayList<>(deps)) {
            builder.addDependency(dep);
        }
        return builder.build();
    }

    private static ResolvedDependencyBuilder app() {
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId("org.acme").setArtifactId("acme-app").setVersion("1.0")
                .setResolvedPaths(PathList.of()).setRuntimeCp();
    }

    private static ResolvedDependencyBuilder runtimeExtension(String artifactId) {
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId("org.acme").setArtifactId(artifactId).setVersion("1.0")
                .setResolvedPaths(PathList.of()).setDependencies(List.of())
                .setRuntimeCp().setDeploymentCp().setRuntimeExtensionArtifact();
    }

    private static ResolvedDependencyBuilder dependency(String artifactId) {
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId("org.acme").setArtifactId(artifactId).setVersion("1.0")
                .setResolvedPaths(PathList.of()).setDependencies(List.of())
                .setDeploymentCp();
    }
}
