package io.quarkus.platform.catalog;

import static io.quarkus.devtools.testing.FakeExtensionCatalog.newFakeExtensionCatalog;
import static io.quarkus.platform.catalog.processor.CatalogProcessor.getProcessedCategoriesInOrder;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.Optional;

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
        final ExtensionCatalog catalog = newFakeExtensionCatalog();
        assertThat(getProcessedCategoriesInOrder(catalog))
                .map(ProcessedCategory::getCategory)
                .map(Category::getId)
                .startsWith("web", "core", "reactive", "serialization", "compatibility", "alt-languages", "uncategorized");
    }

    @Test
    void testExtensionsOrder() {
        final ExtensionCatalog catalog = newFakeExtensionCatalog();
        assertThat(getProcessedCategoriesInOrder(catalog).get(0).getSortedExtensions())
                .map(Extension::getArtifact)
                .map(ArtifactCoords::getArtifactId)
                .startsWith("quarkus-resteasy", "quarkus-resteasy-jackson",
                        "quarkus-resteasy-jsonb", "quarkus-resteasy-reactive");
    }

    @Test
    void testUncategorizedExtensions() {
        final ExtensionCatalog catalog = newFakeExtensionCatalog();
        final Optional<ProcessedCategory> uncategorized = getProcessedCategoriesInOrder(catalog).stream()
                .filter(c -> Objects.equals(c.getCategory().getId(), "uncategorized")).findFirst();
        assertThat(uncategorized).isPresent();
        assertThat(uncategorized.get().getSortedExtensions())
                .map(Extension::getArtifact)
                .map(ArtifactCoords::getArtifactId)
                .contains("quarkus-uncategorized-extension");
    }
}
