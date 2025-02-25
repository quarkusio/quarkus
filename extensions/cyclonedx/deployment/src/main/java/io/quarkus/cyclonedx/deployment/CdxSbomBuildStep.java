package io.quarkus.cyclonedx.deployment;

import java.util.List;

import io.quarkus.cyclonedx.generator.CycloneDxSbomGenerator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AppModelProviderBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.sbom.SbomBuildItem;
import io.quarkus.sbom.ApplicationManifest;

/**
 * Generates SBOMs for packaged applications if the corresponding config is enabled.
 * The API around this is still in development and will likely change in the near future.
 */
public class CdxSbomBuildStep {

    @BuildStep
    public void generate(List<ArtifactResultBuildItem> artifactResultBuildItems,
            OutputTargetBuildItem outputTargetBuildItem,
            AppModelProviderBuildItem appModelProviderBuildItem,
            CycloneDxConfig cdxSbomConfig,
            BuildProducer<SbomBuildItem> sbomProducer) {
        if (cdxSbomConfig.skip()) {
            // until there is a proper way to request the desired build items as build outcome
            return;
        }
        var depInfoProvider = appModelProviderBuildItem.getDependencyInfoProvider().get();
        for (var artifactResult : artifactResultBuildItems) {
            var manifestConfig = artifactResult.getManifestConfig();
            if (manifestConfig != null) {
                var manifest = ApplicationManifest.fromConfig(manifestConfig);
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
}
