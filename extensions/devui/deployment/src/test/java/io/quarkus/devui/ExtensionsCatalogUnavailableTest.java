package io.quarkus.devui;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import tools.jackson.databind.JsonNode;

/**
 * When the project and its extension catalog cannot be resolved, the extension management actions must degrade to
 * empty results instead of failing the JSON-RPC call.
 */
public class ExtensionsCatalogUnavailableTest extends DevUIJsonRPCTest {

    /**
     * Pointing the registry client at a configuration file that does not exist makes every catalog resolution fail
     */
    private static final String TOOLS_CONFIG = "quarkus.tools.config";
    private static final String PREVIOUS_TOOLS_CONFIG = System.getProperty(TOOLS_CONFIG);

    static {
        System.setProperty(TOOLS_CONFIG, "/does/not/exist/quarkus-tools-config.yaml");
    }

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest().withEmptyApplication();

    public ExtensionsCatalogUnavailableTest() {
        super("devui-extensions");
    }

    @AfterAll
    static void restoreToolsConfig() {
        if (PREVIOUS_TOOLS_CONFIG == null) {
            System.clearProperty(TOOLS_CONFIG);
        } else {
            System.setProperty(TOOLS_CONFIG, PREVIOUS_TOOLS_CONFIG);
        }
    }

    @Test
    public void installableExtensionsAreEmptyWhenTheCatalogIsUnavailable() throws Exception {
        JsonNode installable = executeJsonRPCMethod("getInstallableExtensions");
        Assertions.assertNotNull(installable);
        Assertions.assertTrue(installable.isArray(), installable.toString());
        Assertions.assertTrue(installable.isEmpty(), installable.toString());
    }

    @Test
    public void categoriesAreEmptyWhenTheCatalogIsUnavailable() throws Exception {
        JsonNode categories = executeJsonRPCMethod("getCategories");
        Assertions.assertNotNull(categories);
        Assertions.assertTrue(categories.isArray(), categories.toString());
        Assertions.assertTrue(categories.isEmpty(), categories.toString());
    }

    @Test
    public void installedNamespacesAreEmptyWhenTheCatalogIsUnavailable() throws Exception {
        JsonNode namespaces = executeJsonRPCMethod("getInstalledNamespaces");
        Assertions.assertNotNull(namespaces);
        Assertions.assertTrue(namespaces.isArray(), namespaces.toString());
        Assertions.assertTrue(namespaces.isEmpty(), namespaces.toString());
    }

    @Test
    public void addingAnExtensionFailsSoftlyWhenTheCatalogIsUnavailable() throws Exception {
        JsonNode added = executeJsonRPCMethod("addExtension",
                Map.of("extensionArtifactId", "io.quarkus:quarkus-rest"));
        Assertions.assertNotNull(added);
        Assertions.assertFalse(added.asBoolean(true), added.toString());
    }
}
