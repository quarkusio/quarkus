package io.quarkus.cyclonedx.deployment;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.quarkus.cyclonedx.generator.CycloneDxSbomGenerator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AppModelProviderBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.sbom.ApplicationManifestsBuildItem;
import io.quarkus.deployment.sbom.SbomBuildItem;
import io.quarkus.sbom.ApplicationManifest;
import io.quarkus.sbom.ApplicationManifestConfig;
import io.quarkus.sbom.ProductAttribution;

/**
 * Generates SBOMs for packaged applications if the corresponding config is enabled.
 * The API around this is still in development and will likely change in the near future.
 */
public class CdxSbomBuildStep {

    /**
     * Generates CycloneDX SBOMs from application manifests.
     *
     * @param applicationManifestsBuildItem application manifests
     * @param outputTargetBuildItem build output
     * @param appModelProviderBuildItem application model provider
     * @param cdxSbomConfig CycloneDX SBOM generation configuration
     * @param sbomProducer SBOM build item producer
     */
    @BuildStep
    public void generate(ApplicationManifestsBuildItem applicationManifestsBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            AppModelProviderBuildItem appModelProviderBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            CycloneDxConfig cdxSbomConfig,
            BuildProducer<SbomBuildItem> sbomProducer) {
        if (cdxSbomConfig.skip() || applicationManifestsBuildItem.getManifests().isEmpty()) {
            // until there is a proper way to request the desired build items as build outcome
            return;
        }
        var depInfoProvider = appModelProviderBuildItem.getDependencyInfoProvider().get();
        for (var original : applicationManifestsBuildItem.getManifests()) {
            var manifest = cdxSbomConfig.productAttribution()
                    ? ProductAttribution.augment(original, curateOutcomeBuildItem.getApplicationModel())
                    : original;
            for (var sbom : CycloneDxSbomGenerator.newInstance()
                    .setManifest(manifest)
                    .setOutputDirectory(outputTargetBuildItem.getOutputDirectory())
                    .setEffectiveModelResolver(depInfoProvider == null ? null : depInfoProvider.getMavenModelResolver())
                    .setFormat(cdxSbomConfig.format())
                    .setSchemaVersion(cdxSbomConfig.schemaVersion().orElse(null))
                    .setIncludeLicenseText(cdxSbomConfig.includeLicenseText())
                    .generate()) {
                sbomProducer.produce(new SbomBuildItem(sbom));
            }
        }
    }

    /**
     * Embeds a dependency SBOM into the application as a classpath resource when
     * {@code quarkus.cyclonedx.embedded.enabled} is set.
     *
     * @param cdxSbomConfig CycloneDX SBOM generation configuration
     * @param curateOutcomeBuildItem application dependency model
     * @param appModelProviderBuildItem application model provider (for POM metadata resolution)
     * @param generatedResourceProducer producer for the embedded SBOM classpath resource
     */
    @BuildStep
    public void embedDependencySbom(CycloneDxConfig cdxSbomConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            AppModelProviderBuildItem appModelProviderBuildItem,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer) {
        if (!cdxSbomConfig.embedded().enabled()) {
            return;
        }
        final String resourceName = cdxSbomConfig.embedded().resourceName();
        final var model = curateOutcomeBuildItem.getApplicationModel();
        // a dependency SBOM describes the application dependencies, so it is built from the model
        // without a distribution directory
        ApplicationManifest manifest = ApplicationManifest.fromConfig(
                ApplicationManifestConfig.builder().setApplicationModel(model).build());
        if (cdxSbomConfig.productAttribution()) {
            manifest = ProductAttribution.augment(manifest, model);
        }
        // the dependency info provider (used to resolve POM metadata) may be unavailable, e.g. in tests
        final var depInfoProviderSupplier = appModelProviderBuildItem.getDependencyInfoProvider();
        final var depInfoProvider = depInfoProviderSupplier == null ? null : depInfoProviderSupplier.get();
        final List<String> sboms = CycloneDxSbomGenerator.newInstance()
                .setManifest(manifest)
                .setFormat(formatOf(resourceName))
                .setSchemaVersion(cdxSbomConfig.schemaVersion().orElse(null))
                .setIncludeLicenseText(cdxSbomConfig.includeLicenseText())
                .setEffectiveModelResolver(depInfoProvider == null ? null : depInfoProvider.getMavenModelResolver())
                .generateText();
        generatedResourceProducer.produce(
                new GeneratedResourceBuildItem(resourceName, sboms.get(0).getBytes(StandardCharsets.UTF_8)));
    }

    private static String formatOf(String resourceName) {
        return resourceName.endsWith(".xml") ? "xml" : "json";
    }
}
