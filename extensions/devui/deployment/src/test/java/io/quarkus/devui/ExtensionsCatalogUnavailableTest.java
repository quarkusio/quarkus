package io.quarkus.devui;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import tools.jackson.databind.JsonNode;

/**
 * When the project and its extension catalog cannot be resolved, the extension management actions must degrade
 * instead of failing the JSON-RPC call: the listings answer empty, {@code getInstalledNamespaces} answers null so
 * that the Dev UI keeps the extension management UI hidden, and adding an extension answers false.
 */
public class ExtensionsCatalogUnavailableTest extends DevUIJsonRPCTest {

    /**
     * Pointing the registry client at a configuration file that does not exist makes every catalog resolution fail.
     * This is read by {@code RegistriesConfigLocator} before any project or Maven work happens, so the actions fail
     * where the reported issue fails them, before they can touch the build file of the surrounding project.
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

    @BeforeAll
    static void requireTheConfigFileToBeConsulted() {
        // QUARKUS_REGISTRIES short circuits RegistriesConfigLocator, which would leave the catalog resolvable and
        // the failure uninjected.
        Assumptions.assumeTrue(System.getenv("QUARKUS_REGISTRIES") == null,
                "QUARKUS_REGISTRIES overrides the registry client configuration, the failure cannot be injected");
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

    /**
     * Not an empty list: the Dev UI enables extension management on any result this action returns.
     */
    @Test
    public void installedNamespacesAreNullWhenTheCatalogIsUnavailable() throws Exception {
        JsonNode namespaces = executeJsonRPCMethod("getInstalledNamespaces");
        Assertions.assertNotNull(namespaces);
        Assertions.assertTrue(namespaces.isNull(), namespaces.toString());
    }

    @Test
    public void addingAnExtensionFailsSoftlyWhenTheCatalogIsUnavailable() throws Exception {
        // AddExtensions writes to the build file of the project it is given, which here is the module this test
        // runs in. Only exercise it once the read only actions have shown that the catalog really is unavailable.
        JsonNode installable = executeJsonRPCMethod("getInstallableExtensions");
        Assumptions.assumeTrue(installable != null && installable.isArray() && installable.isEmpty(),
                "The extension catalog resolved, the unavailable path cannot be exercised safely");

        JsonNode added = executeJsonRPCMethod("addExtension",
                Map.of("extensionArtifactId", "io.quarkus:quarkus-rest"));
        Assertions.assertNotNull(added);
        Assertions.assertFalse(added.asBoolean(true), added.toString());
    }
}
