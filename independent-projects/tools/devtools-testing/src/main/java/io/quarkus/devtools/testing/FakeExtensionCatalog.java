package io.quarkus.devtools.testing;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public final class FakeExtensionCatalog {

    private static final String FAKE_EXTENSION_CATALOG_PATH = "/fake-catalog.json";
    public static final ExtensionCatalog FAKE_EXTENSION_CATALOG = newFakeExtensionCatalog();
    public static final QuarkusCodestartCatalog FAKE_QUARKUS_CODESTART_CATALOG = getQuarkusCodestartCatalog();

    private FakeExtensionCatalog() {
    }

    private static QuarkusCodestartCatalog getQuarkusCodestartCatalog() {
        try {
            return QuarkusCodestartCatalog.fromBaseCodestartsResources(
                    QuarkusCodestartCatalog.buildExtensionsMapping(FAKE_EXTENSION_CATALOG.getExtensions()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static JsonExtensionCatalog newFakeExtensionCatalog() {
        InputStream inputString = FakeExtensionCatalog.class.getResourceAsStream(FAKE_EXTENSION_CATALOG_PATH);
        if (inputString == null) {
            throw new IllegalStateException("Failed to locate " + FAKE_EXTENSION_CATALOG_PATH + " on the classpath");
        }
        try {
            return JsonCatalogMapperHelper.deserialize(inputString, JsonExtensionCatalog.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
