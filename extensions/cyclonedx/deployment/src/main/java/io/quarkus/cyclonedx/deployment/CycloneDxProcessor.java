package io.quarkus.cyclonedx.deployment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import io.quarkus.bootstrap.app.DependencyInfoProvider;
import io.quarkus.bootstrap.app.SbomResult;
import io.quarkus.cyclonedx.deployment.spi.EmbeddedSbomMetadataBuildItem;
import io.quarkus.cyclonedx.deployment.spi.EmbeddedSbomRequestBuildItem;
import io.quarkus.cyclonedx.generator.CycloneDxSbomGenerator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AppModelProviderBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.sbom.SbomBuildItem;
import io.quarkus.deployment.sbom.SbomContributionBuildItem;
import io.quarkus.sbom.CoreSbomContributionConfig;
import io.quarkus.sbom.SbomContribution;

/**
 * Generates CycloneDX SBOMs for Quarkus applications.
 */
public class CycloneDxProcessor {

    /**
     * Generates CycloneDX SBOM files for the packaged application.
     * <p>
     * Assembles the core application contribution from {@link ArtifactResultBuildItem}
     * (which includes distribution paths resolved during packaging) and merges it with
     * extension contributions from {@link SbomContributionBuildItem}.
     *
     * @param outputTargetBuildItem build output directory
     * @param appModelProviderBuildItem application model provider for POM metadata resolution
     * @param artifactResultBuildItems packaged artifact results carrying {@link CoreSbomContributionConfig}
     * @param sbomContributions extension SBOM contributions (npm, PyPI, etc.)
     * @param cdxSbomConfig CycloneDX SBOM generation configuration
     * @param sbomProducer SBOM build item producer
     */
    @BuildStep
    public void generate(OutputTargetBuildItem outputTargetBuildItem,
            AppModelProviderBuildItem appModelProviderBuildItem,
            List<ArtifactResultBuildItem> artifactResultBuildItems,
            List<SbomContributionBuildItem> sbomContributions,
            CycloneDxConfig cdxSbomConfig,
            BuildProducer<SbomBuildItem> sbomProducer) {
        if (!cdxSbomConfig.enabled() || artifactResultBuildItems.isEmpty()) {
            return;
        }
        var depInfoProvider = getDependencyInfoProvider(appModelProviderBuildItem);
        for (ArtifactResultBuildItem artifactResult : artifactResultBuildItems) {
            CoreSbomContributionConfig manifestConfig = artifactResult.getCoreSbomConfig();
            if (manifestConfig == null) {
                continue;
            }
            List<SbomContribution> contributions = collectContributions(manifestConfig.toSbomContribution(), sbomContributions);
            for (SbomResult sbom : CycloneDxSbomGenerator.newInstance()
                    .setOutputDirectory(outputTargetBuildItem.getOutputDirectory())
                    .setEffectiveModelResolver(depInfoProvider == null ? null : depInfoProvider.getMavenModelResolver())
                    .setFormat(cdxSbomConfig.format())
                    .setSchemaVersion(cdxSbomConfig.schemaVersion().orElse(null))
                    .setIncludeLicenseText(cdxSbomConfig.includeLicenseText())
                    .setPrettyPrint(cdxSbomConfig.prettyPrint())
                    .setContributions(contributions)
                    .generate()) {
                sbomProducer.produce(new SbomBuildItem(sbom));
            }
        }
    }

    private static DependencyInfoProvider getDependencyInfoProvider(AppModelProviderBuildItem appModelProviderBuildItem) {
        var supplier = appModelProviderBuildItem.getDependencyInfoProvider();
        return supplier == null ? null : supplier.get();
    }

    private static List<SbomContribution> collectContributions(SbomContribution coreContribution,
            List<SbomContributionBuildItem> contributions) {
        if (contributions.isEmpty()) {
            return List.of(coreContribution);
        }
        List<SbomContribution> result = new ArrayList<>(contributions.size() + 1);
        result.add(coreContribution);
        for (SbomContributionBuildItem contribution : contributions) {
            result.add(contribution.getContribution());
        }
        return result;
    }

    /**
     * Embeds a dependency SBOM as a compressed resource inside the application artifact.
     * <p>
     * Assembles the core application contribution from {@link CurateOutcomeBuildItem}
     * (without distribution paths, since the embedded SBOM describes dependencies rather
     * than the distribution layout) and merges it with extension contributions from
     * {@link SbomContributionBuildItem}.
     * <p>
     * This step does not depend on {@link ArtifactResultBuildItem}, avoiding a build cycle
     * with the jar packaging steps.
     *
     * @param generatedResourceBuildItem producer for the embedded SBOM resource
     * @param embeddedSbomMetadataProducer producer for embedded SBOM metadata
     * @param cdxConfig CycloneDX SBOM configuration
     * @param curateOutcomeBuildItem application dependency model
     * @param appModelProviderBuildItem application model provider for POM metadata resolution
     * @param embeddedSbomRequests explicit requests to embed an SBOM
     * @param sbomContributions extension SBOM contributions (npm, PyPI, etc.)
     */
    @BuildStep
    public void embedDependencySbom(BuildProducer<GeneratedResourceBuildItem> generatedResourceBuildItem,
            BuildProducer<EmbeddedSbomMetadataBuildItem> embeddedSbomMetadataProducer,
            CycloneDxConfig cdxConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            AppModelProviderBuildItem appModelProviderBuildItem,
            List<EmbeddedSbomRequestBuildItem> embeddedSbomRequests,
            List<SbomContributionBuildItem> sbomContributions) {
        if (!cdxConfig.enabled() || !cdxConfig.embedded().enabled() && embeddedSbomRequests.isEmpty()) {
            return;
        }

        final CycloneDxConfig.EmbeddedSbomConfig dependencySbomConfig = cdxConfig.embedded();
        final String resourceName = dependencySbomConfig.resourceName();
        if (resourceName == null || resourceName.isEmpty()) {
            throw new IllegalArgumentException("resourceName is not configured for the embedded dependency SBOM");
        }

        SbomContribution coreContribution = CoreSbomContributionConfig.builder()
                .setApplicationModel(curateOutcomeBuildItem.getApplicationModel())
                .build()
                .toSbomContribution();

        var depInfoProvider = getDependencyInfoProvider(appModelProviderBuildItem);
        List<String> result = CycloneDxSbomGenerator.newInstance()
                .setEffectiveModelResolver(depInfoProvider == null ? null : depInfoProvider.getMavenModelResolver())
                .setFormat(getFormat(resourceName))
                .setSchemaVersion(cdxConfig.schemaVersion().orElse(null))
                .setIncludeLicenseText(cdxConfig.includeLicenseText())
                .setPrettyPrint(cdxConfig.prettyPrint())
                .setContributions(collectContributions(coreContribution, sbomContributions))
                .generateText();

        if (result.size() != 1) {
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
