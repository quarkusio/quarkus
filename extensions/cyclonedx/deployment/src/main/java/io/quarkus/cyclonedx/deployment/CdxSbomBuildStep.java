package io.quarkus.cyclonedx.deployment;

import io.quarkus.cyclonedx.generator.CycloneDxSbomGenerator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AppModelProviderBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.sbom.ApplicationManifestsBuildItem;
import io.quarkus.deployment.sbom.SbomBuildItem;

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
            CycloneDxConfig cdxSbomConfig,
            BuildProducer<SbomBuildItem> sbomProducer) {
        if (cdxSbomConfig.skip() || applicationManifestsBuildItem.getManifests().isEmpty()) {
            // until there is a proper way to request the desired build items as build outcome
            return;
        }
        var depInfoProvider = appModelProviderBuildItem.getDependencyInfoProvider().get();
        for (var manifest : applicationManifestsBuildItem.getManifests()) {
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
}
