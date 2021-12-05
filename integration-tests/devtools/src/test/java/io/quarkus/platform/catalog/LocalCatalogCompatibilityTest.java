package io.quarkus.platform.catalog;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.PlatformAwareTestBase;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;

public class LocalCatalogCompatibilityTest extends PlatformAwareTestBase {

    @Test
    void testCatalog() throws RegistryResolutionException, IOException {
        final ExtensionCatalogResolver catalogResolver = QuarkusProjectHelper.getCatalogResolver();
        RegistrySnapshotCatalogCompatibilityTest.testPlatformCatalog(catalogResolver, catalogResolver.resolvePlatformCatalog(),
                "io.quarkus");
    }
}
