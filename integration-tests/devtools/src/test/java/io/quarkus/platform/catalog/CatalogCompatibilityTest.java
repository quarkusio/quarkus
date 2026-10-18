package io.quarkus.platform.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.CodestartResourceLoadersBuilder;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.PlatformAwareTestBase;
import io.quarkus.platform.catalog.processor.CatalogProcessor;
import io.quarkus.platform.catalog.processor.ExtensionProcessor;
import io.quarkus.platform.catalog.processor.ProcessedCategory;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformStream;

public class CatalogCompatibilityTest extends PlatformAwareTestBase {

    @Test
    void testCatalog() throws RegistryResolutionException, IOException {
        final ExtensionCatalogResolver catalogResolver = QuarkusProjectHelper.getCatalogResolver();
        testPlatformCatalog(catalogResolver, catalogResolver.resolvePlatformCatalog(),
                "io.quarkus");
    }

    static void testPlatformCatalog(ExtensionCatalogResolver catalogResolver, PlatformCatalog platformCatalog,
            String expectedPlatformKey)
            throws RegistryResolutionException, IOException {
        assertThat(platformCatalog.getPlatforms())
                .extracting(Platform::getPlatformKey)
                .isNotEmpty()
                .contains(expectedPlatformKey);
        final Platform platform = platformCatalog.getPlatform(expectedPlatformKey);
        assertThat(platform).isNotNull();
        assertThat(platform.getStreams()).isNotEmpty();
        for (PlatformStream s : platform.getStreams()) {
            for (PlatformRelease r : s.getReleases()) {
                checkPlatformRelease(catalogResolver, r);
            }
        }
    }

    private static void checkPlatformRelease(ExtensionCatalogResolver catalogResolver, PlatformRelease r)
            throws RegistryResolutionException, IOException {
        assertThat(r).isNotNull();
        final ExtensionCatalog extensionCatalog = catalogResolver.resolveExtensionCatalog(r.getMemberBoms());
        final CatalogProcessor processed = CatalogProcessor.of(extensionCatalog);
        for (ProcessedCategory cat : processed.getProcessedCategoriesInOrder()) {
            for (Extension e : cat.getSortedExtensions()) {
                checkExtensionProcessor(e);
            }
        }
        checkCodestarts(extensionCatalog, processed);
    }

    private static void checkCodestarts(ExtensionCatalog extensionCatalog, CatalogProcessor processed) throws IOException {
        final List<ResourceLoader> codestartResourceLoaders = CodestartResourceLoadersBuilder
                .getCodestartResourceLoaders(MessageWriter.info(), extensionCatalog);
        final QuarkusCodestartCatalog quarkusCodestartCatalog = QuarkusCodestartCatalog
                .fromExtensionsCatalog(extensionCatalog, codestartResourceLoaders);
        assertThat(quarkusCodestartCatalog.getCodestarts()).isNotNull();
        assertThat(processed.getCodestartArtifacts()).isNotEmpty();
    }

    private static void checkExtensionProcessor(Extension e) {
        final ExtensionProcessor extensionProcessor = ExtensionProcessor.of(e);
        assertThat(extensionProcessor.getCategories()).isNotNull();
        assertThat(extensionProcessor.getExtendedKeywords()).isNotNull();
        assertThat(extensionProcessor.getCodestartLanguages()).isNotNull();
        assertThat(extensionProcessor.getSyntheticMetadata()).isNotNull();
        assertThat(extensionProcessor.getKeywords()).isNotNull();

        // Just check that no exception is thrown
        extensionProcessor.getBuiltWithQuarkusCore();
        extensionProcessor.getGuide();
        extensionProcessor.getCodestartArtifact();
        extensionProcessor.getCodestartKind();
        extensionProcessor.getCodestartName();
        extensionProcessor.getShortName();
        extensionProcessor.isUnlisted();
        extensionProcessor.providesCode();
        extensionProcessor.getBom();
        extensionProcessor.getNonQuarkusBomOnly();
    }

}
