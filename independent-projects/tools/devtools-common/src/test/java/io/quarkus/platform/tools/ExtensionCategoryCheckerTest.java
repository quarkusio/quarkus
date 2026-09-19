package io.quarkus.platform.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public class ExtensionCategoryCheckerTest {

    private static final MessageWriter SILENT = new MessageWriter() {
        @Override
        public void info(String msg) {
        }

        @Override
        public void error(String msg) {
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(String msg) {
        }

        @Override
        public void warn(String msg) {
        }
    };

    @TempDir
    Path tempDir;

    private String previousRegistryClientProperty;

    @BeforeEach
    public void disableRegistryClient() {
        // Every test in this class exercises the constructor's fallback without network access
        previousRegistryClientProperty = System.getProperty("quarkusRegistryClient");
        System.setProperty("quarkusRegistryClient", "false");
        QuarkusProjectHelper.reset();
    }

    @AfterEach
    public void restoreRegistryClient() {
        if (previousRegistryClientProperty == null) {
            System.clearProperty("quarkusRegistryClient");
        } else {
            System.setProperty("quarkusRegistryClient", previousRegistryClientProperty);
        }
        QuarkusProjectHelper.reset();
    }

    @Test
    public void usesLocalFileWhenPresent() throws IOException {
        Path file = writeCatalogOverrides("""
                {
                  "categories": [
                    {"id": "web", "name": "Web Name"},
                    {"id": "data", "name": "Data With Stuff"}
                  ]
                }
                """);
        ExtensionCategoryChecker checker = new ExtensionCategoryChecker(file, SILENT);

        assertThat(checker.findUnknownCategories(extensionDescriptor("web", "logging")))
                .containsExactly("logging");
    }

    @Test
    public void hasNoKnownCategoriesWhenFileIsNullAndRegistryClientDisabled() throws IOException {
        ExtensionCategoryChecker checker = new ExtensionCategoryChecker(SILENT);

        assertThat(checker.findUnknownCategories(extensionDescriptor("not-a-real-category"))).isEmpty();
    }

    @Test
    public void hasNoKnownCategoriesWhenFileDoesNotExistAndRegistryClientDisabled() throws IOException {
        ExtensionCategoryChecker checker = new ExtensionCategoryChecker(tempDir.resolve("does-not-exist.json"), SILENT);

        assertThat(checker.findUnknownCategories(extensionDescriptor("not-a-real-category"))).isEmpty();
    }

    @Test
    public void fallsBackWhenLocalFileListsNoCategories() throws IOException {
        Path file = writeCatalogOverrides("{\"categories\":[]}");
        ExtensionCategoryChecker checker = new ExtensionCategoryChecker(file, SILENT);

        assertThat(checker.findUnknownCategories(extensionDescriptor("not-a-real-category"))).isEmpty();
    }

    @Test
    public void findUnknownCategoriesReturnsEmptyWhenAllCategoriesAreKnown() throws IOException {
        Path file = writeCatalogOverrides("{\"categories\":[{\"id\":\"web\"},{\"id\":\"data\"}]}");
        ExtensionCategoryChecker checker = new ExtensionCategoryChecker(file, SILENT);

        assertThat(checker.findUnknownCategories(extensionDescriptor("web", "data"))).isEmpty();
    }

    @Test
    public void findUnknownCategoriesReturnsEmptyWhenExtensionHasNoCategories() throws IOException {
        Path file = writeCatalogOverrides("{\"categories\":[{\"id\":\"web\"}]}");
        ExtensionCategoryChecker checker = new ExtensionCategoryChecker(file, SILENT);

        ObjectNode extObject = JsonMapper.builder().build().createObjectNode();

        assertThat(checker.findUnknownCategories(extObject)).isEmpty();
    }

    @Test
    public void warningForMentionsTheUnknownCategory() {
        assertThat(ExtensionCategoryChecker.warningFor("logging")).contains("'logging'");
    }

    private Path writeCatalogOverrides(String content) throws IOException {
        Path file = tempDir.resolve("catalog-overrides.json");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private ObjectNode extensionDescriptor(String... categories) {
        ObjectNode extObject = JsonMapper.builder().build().createObjectNode();
        ObjectNode metadata = extObject.putObject("metadata");
        var categoriesArray = metadata.putArray("categories");
        for (String category : categories) {
            categoriesArray.add(category);
        }
        return extObject;
    }
}
