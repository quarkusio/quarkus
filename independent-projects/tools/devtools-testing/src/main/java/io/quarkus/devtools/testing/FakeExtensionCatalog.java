package io.quarkus.devtools.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.registry.catalog.ExtensionCatalog;

public final class FakeExtensionCatalog {

    private static final String FAKE_EXTENSION_CATALOG_PATH = "/fake-catalog.json";
    public static final ExtensionCatalog FAKE_EXTENSION_CATALOG = newFakeExtensionCatalog();
    public static final QuarkusCodestartCatalog FAKE_QUARKUS_CODESTART_CATALOG = getQuarkusCodestartCatalog();

    private FakeExtensionCatalog() {
    }

    private static QuarkusCodestartCatalog getQuarkusCodestartCatalog() {
        try {
            return QuarkusCodestartCatalog.fromBaseCodestartsResources(
                    MessageWriter.info(),
                    QuarkusCodestartCatalog.buildExtensionsMapping(FAKE_EXTENSION_CATALOG.getExtensions()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ExtensionCatalog newFakeExtensionCatalog() {
        InputStream inputString = FakeExtensionCatalog.class.getResourceAsStream(FAKE_EXTENSION_CATALOG_PATH);
        if (inputString == null) {
            throw new IllegalStateException("Failed to locate " + FAKE_EXTENSION_CATALOG_PATH + " on the classpath");
        }
        try {
            return ExtensionCatalog.fromStream(inputString);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String getDefaultCodestart() {
        var map = FAKE_EXTENSION_CATALOG.getMetadata().get("project");
        if (map != null) {
            if (map instanceof Map) {
                var defaultCodestart = ((Map<?, ?>) map).get("default-codestart");
                if (defaultCodestart instanceof String) {
                    return defaultCodestart.toString();
                }
            }
        }
        return null;
    }
}
