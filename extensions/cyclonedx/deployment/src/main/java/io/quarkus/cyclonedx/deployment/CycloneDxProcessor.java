package io.quarkus.cyclonedx.deployment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import io.quarkus.bootstrap.app.DependencyInfoProvider;
import io.quarkus.cyclonedx.deployment.spi.EmbeddedSbomMetadataBuildItem;
import io.quarkus.cyclonedx.deployment.spi.EmbeddedSbomRequestBuildItem;
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

/**
 * Generates SBOMs for packaged applications if the corresponding config is enabled.
 * The API around this is still in development and will likely change in the near future.
 */
public class CycloneDxProcessor {

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
        if (!cdxSbomConfig.enabled() || applicationManifestsBuildItem.getManifests().isEmpty()) {
            // until there is a proper way to request the desired build items as build outcome
            return;
        }
        var depInfoProvider = getDependencyInfoProvider(appModelProviderBuildItem);
        for (var manifest : applicationManifestsBuildItem.getManifests()) {
            for (var sbom : CycloneDxSbomGenerator.newInstance()
                    .setManifest(manifest)
                    .setOutputDirectory(outputTargetBuildItem.getOutputDirectory())
                    .setEffectiveModelResolver(depInfoProvider == null ? null : depInfoProvider.getMavenModelResolver())
                    .setFormat(cdxSbomConfig.format())
                    .setSchemaVersion(cdxSbomConfig.schemaVersion().orElse(null))
                    .setIncludeLicenseText(cdxSbomConfig.includeLicenseText())
                    .setPrettyPrint(cdxSbomConfig.prettyPrint())
                    .generate()) {
                sbomProducer.produce(new SbomBuildItem(sbom));
            }
        }
    }

    private static DependencyInfoProvider getDependencyInfoProvider(AppModelProviderBuildItem appModelProviderBuildItem) {
        var supplier = appModelProviderBuildItem.getDependencyInfoProvider();
        return supplier == null ? null : supplier.get();
    }

    @BuildStep
    public void embedDependencySbom(BuildProducer<GeneratedResourceBuildItem> generatedResourceBuildItem,
            BuildProducer<EmbeddedSbomMetadataBuildItem> embeddedSbomMetadataProducer,
            CycloneDxConfig cdxConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            AppModelProviderBuildItem appModelProviderBuildItem,
            List<EmbeddedSbomRequestBuildItem> embeddedSbomRequests) {
        if (!cdxConfig.enabled() || !cdxConfig.embedded().enabled() && embeddedSbomRequests.isEmpty()) {
            return;
        }

        final CycloneDxConfig.EmbeddedSbomConfig dependencySbomConfig = cdxConfig.embedded();
        final String resourceName = dependencySbomConfig.resourceName();
        if (resourceName == null || resourceName.isEmpty()) {
            throw new IllegalArgumentException("resourceName is not configured for the embedded dependency SBOM");
        }

        var depInfoProvider = getDependencyInfoProvider(appModelProviderBuildItem);
        List<String> result = CycloneDxSbomGenerator.newInstance()
                .setManifest(ApplicationManifest.fromConfig(ApplicationManifestConfig.builder()
                        .setApplicationModel(curateOutcomeBuildItem.getApplicationModel())
                        .build()))
                .setEffectiveModelResolver(depInfoProvider == null ? null : depInfoProvider.getMavenModelResolver())
                .setFormat(getFormat(resourceName))
                .setSchemaVersion(cdxConfig.schemaVersion().orElse(null))
                .setIncludeLicenseText(cdxConfig.includeLicenseText())
                .setPrettyPrint(cdxConfig.prettyPrint())
                .generateText();

        if (result.size() != 1) {
            // this should never happen since the format is derived from the resourceName
            throw new RuntimeException(
                    "Embedded dependency SBOM has more than 1 result for configured resource " + resourceName);
        }

        byte[] sbomBytes = result.get(0).getBytes(StandardCharsets.UTF_8);
        final boolean compressed = dependencySbomConfig.compress();
        String effectiveResourceName = resourceName;
        if (compressed) {
            sbomBytes = gzip(sbomBytes);
            if (!resourceName.endsWith(".gz")) {
                effectiveResourceName = resourceName + ".gz";
            }
        }

        generatedResourceBuildItem.produce(new GeneratedResourceBuildItem(effectiveResourceName, sbomBytes));
        embeddedSbomMetadataProducer.produce(new EmbeddedSbomMetadataBuildItem(effectiveResourceName, compressed));
    }

    private static byte[] gzip(byte[] data) {
        var baos = new ByteArrayOutputStream(data.length);
        try (var gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to GZIP-compress the embedded SBOM", e);
        }
        return baos.toByteArray();
    }

    private static String getFormat(String resourceName) {
        int lastDot = resourceName.lastIndexOf('.');
        return lastDot == -1 ? "json" : resourceName.substring(lastDot + 1);
    }
}
