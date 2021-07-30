package io.quarkus.platform.catalog;

import static io.quarkus.platform.catalog.processor.CatalogProcessor.getProcessedCategoriesInOrder;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.PlatformAwareTestBase;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.platform.catalog.processor.ProcessedCategory;
import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;

public class CatalogProcessorTest extends PlatformAwareTestBase {

    @Test
    void testCategoryOrder() {
        final ExtensionCatalog catalog = getExtensionsCatalog();
        assertThat(getProcessedCategoriesInOrder(catalog))
                .map(ProcessedCategory::getCategory)
                .map(Category::getId)
                .startsWith("web", "data", "messaging", "core");
    }

    @Test
    void testExtensionsOrder() {
        final ExtensionCatalog catalog = getExtensionsCatalog();
        assertThat(getProcessedCategoriesInOrder(catalog).get(0).getSortedExtensions())
                .map(Extension::getArtifact)
                .map(ArtifactCoords::getArtifactId)
                .startsWith("quarkus-resteasy", "quarkus-resteasy-jackson",
                        "quarkus-resteasy-jsonb", "quarkus-apache-httpclient",
                        "quarkus-vertx-http", "quarkus-vertx-graphql",
                        "quarkus-grpc", "quarkus-grpc-common",
                        "quarkus-hibernate-validator");
    }
}
