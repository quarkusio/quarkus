package io.quarkus.platform.catalog.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;

class ProcessedCategoryTest {

    @Test
    void should_sort_extensions_ignoring_case() {
        Extension zipFile = createExtension("Camel Zip File");
        Extension gRPC = createExtension("Camel gRPC");
        Extension iCal = createExtension("Camel iCal");
        Extension univocityParser = createExtension("Camel univocityParser");
        ProcessedCategory category = new ProcessedCategory(Category.builder().setId("integration").build(), List.of(
                zipFile,
                gRPC,
                iCal,
                univocityParser));
        assertThat(category.getSortedExtensions()).containsExactly(gRPC, iCal, univocityParser, zipFile);
    }

    private Extension createExtension(String name) {
        return Extension.builder()
                .setGroupId("org.foo")
                .setArtifactId("bar-" + name)
                .setName(name)
                .build();
    }
}
