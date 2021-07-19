package io.quarkus.platform.catalog.processor;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.json.JsonCategory;
import io.quarkus.registry.catalog.json.JsonExtension;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessedCategoryTest {

    @Test
    void should_sort_extensions_ignoring_case() {
        Extension zipFile = createExtension("Camel Zip File");
        Extension gRPC = createExtension("Camel gRPC");
        Extension iCal = createExtension("Camel iCal");
        Extension univocityParser = createExtension("Camel univocityParser");
        ProcessedCategory category = new ProcessedCategory(new JsonCategory(), List.of(
                zipFile,
                gRPC,
                iCal,
                univocityParser));
        assertThat(category.getSortedExtensions()).containsExactly(gRPC, iCal, univocityParser, zipFile);
    }

    private Extension createExtension(String name) {
        JsonExtension extension = new JsonExtension();
        extension.setGroupId("org.foo");
        extension.setArtifactId("bar-" + name);
        extension.setName(name);
        return extension;
    }
}
