package io.quarkus.cyclonedx.deployment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import io.quarkus.deployment.pkg.builditem.JarTreeShakeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.sbom.SbomBuildItem;
import io.quarkus.deployment.sbom.SbomContributionBuildItem;
import io.quarkus.deployment.sbom.SbomGeneratedResourceBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
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
     * Generates the embedded dependency SBOM resource.
     * <p>
     * Assembles the core application contribution from {@link CurateOutcomeBuildItem}
     * (without distribution paths, since the embedded SBOM describes dependencies rather
     * than the distribution layout), applies tree-shake pedigree notes from
     * {@link JarTreeShakeBuildItem}, and merges with extension contributions from
     * {@link SbomContributionBuildItem}.
     * <p>
     * This step produces {@link SbomGeneratedResourceBuildItem} instead of
     * {@link GeneratedResourceBuildItem} to avoid a build-step cycle with tree-shake
     * root collection. The JAR build step merges these into the generated resources
     * passed to JAR builders. It does not produce {@link EmbeddedSbomMetadataBuildItem}
     * either, since that feeds into endpoint registration and would create a separate
     * cycle through the main class build step; see {@link #embedDependencySbomMetadata}.
     *
     * @param sbomResourceProducer producer for the embedded SBOM resource
     * @param cdxConfig CycloneDX SBOM configuration
     * @param curateOutcomeBuildItem application dependency model
     * @param appModelProviderBuildItem application model provider for POM metadata resolution
     * @param treeShakeResult tree-shake results used to compute pedigree notes for shaken dependencies
     * @param embeddedSbomRequests explicit requests to embed an SBOM
     * @param sbomContributions extension SBOM contributions (npm, PyPI, etc.)
     */
    @BuildStep
    public void generateEmbeddedSbom(BuildProducer<SbomGeneratedResourceBuildItem> sbomResourceProducer,
            CycloneDxConfig cdxConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            AppModelProviderBuildItem appModelProviderBuildItem,
            JarTreeShakeBuildItem treeShakeResult,
            List<EmbeddedSbomRequestBuildItem> embeddedSbomRequests,
            List<SbomContributionBuildItem> sbomContributions) {
        if (!isEmbeddedSbomEnabled(cdxConfig, embeddedSbomRequests)) {
            return;
        }

        byte[] sbomBytes = generateEmbeddedSbomBytes(cdxConfig, curateOutcomeBuildItem, appModelProviderBuildItem,
                computePedigrees(treeShakeResult), sbomContributions);
        String effectiveResourceName = effectiveResourceName(cdxConfig.embedded());
        if (cdxConfig.embedded().compress()) {
            sbomBytes = gzip(sbomBytes);
        }

        sbomResourceProducer.produce(new SbomGeneratedResourceBuildItem(effectiveResourceName, sbomBytes));
    }

    private byte[] generateEmbeddedSbomBytes(CycloneDxConfig cdxConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            AppModelProviderBuildItem appModelProviderBuildItem,
            Map<ArtifactKey, String> pedigrees,
            List<SbomContributionBuildItem> sbomContributions) {
        final String resourceName = cdxConfig.embedded().resourceName();

        CoreSbomContributionConfig config = new CoreSbomContributionConfig()
                .setApplicationModel(curateOutcomeBuildItem.getApplicationModel())
                .setPedigrees(pedigrees);
        SbomContribution coreContribution = config.toSbomContribution();

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

        return result.get(0).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Produces {@link EmbeddedSbomMetadataBuildItem} for the embedded SBOM.
     * <p>
     * Separated from {@link #generateEmbeddedSbom} so that metadata production does not
     * depend on {@link JarTreeShakeBuildItem}, avoiding a build-step cycle through
     * endpoint registration and the main class build step.
     */
    @BuildStep
    public void embedDependencySbomMetadata(
            BuildProducer<EmbeddedSbomMetadataBuildItem> embeddedSbomMetadataProducer,
            CycloneDxConfig cdxConfig,
            List<EmbeddedSbomRequestBuildItem> embeddedSbomRequests) {
        if (!isEmbeddedSbomEnabled(cdxConfig, embeddedSbomRequests)) {
            return;
        }
        final CycloneDxConfig.EmbeddedSbomConfig embeddedConfig = cdxConfig.embedded();
        embeddedSbomMetadataProducer
                .produce(new EmbeddedSbomMetadataBuildItem(effectiveResourceName(embeddedConfig), embeddedConfig.compress()));
    }

    private static boolean isEmbeddedSbomEnabled(CycloneDxConfig cdxConfig,
            List<EmbeddedSbomRequestBuildItem> embeddedSbomRequests) {
        if (!cdxConfig.enabled()) {
            return false;
        }
        if (!cdxConfig.embedded().enabled() && embeddedSbomRequests.isEmpty()) {
            return false;
        }
        final String resourceName = cdxConfig.embedded().resourceName();
        if (resourceName == null || resourceName.isEmpty()) {
            throw new IllegalArgumentException("resourceName is not configured for the embedded dependency SBOM");
        }
        return true;
    }

    private static String effectiveResourceName(CycloneDxConfig.EmbeddedSbomConfig config) {
        String resourceName = config.resourceName();
        if (config.compress() && !resourceName.endsWith(".gz")) {
            return resourceName + ".gz";
        }
        return resourceName;
    }

    private static Map<ArtifactKey, String> computePedigrees(JarTreeShakeBuildItem treeShakeResult) {
        if (!treeShakeResult.isClassesShaken()) {
            return null;
        }
        Map<ArtifactKey, String> pedigrees = new HashMap<>();
        for (ArtifactKey key : treeShakeResult.getRemovedClasses().keySet()) {
            String pedigree = treeShakeResult.computePedigree(key);
            if (pedigree != null) {
                pedigrees.put(key, pedigree);
            }
        }
        return pedigrees.isEmpty() ? null : pedigrees;
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
