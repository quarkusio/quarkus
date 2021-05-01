package io.quarkus.platform.catalog.compatibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonExtension;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;

public class ExtensionCatalogCompatibilityTest {

    @Test
    public void testEmpty() throws Exception {

        final JsonExtensionCatalog catalog = new JsonExtensionCatalog();

        catalog.addExtension(new ExtensionBuilder(ArtifactCoords.fromString("org.acme:a-classic:1.0"))
                .addCapability("classic")
                .build());

        catalog.addExtension(new ExtensionBuilder(ArtifactCoords.fromString("org.acme:a-reactive:1.0"))
                .addCapability("reactive")
                .build());

        final ExtensionCatalogCompatibility catalogCompat = ExtensionCatalogCompatibility.forCatalog(catalog);
        assertTrue(catalogCompat.isEmpty());
    }

    @Test
    public void testDirectCapabilityConflict() throws Exception {

        final JsonExtensionCatalog catalog = new JsonExtensionCatalog();

        final ArtifactCoords aClassic = ArtifactCoords.fromString("org.acme:a-classic:1.0");
        catalog.addExtension(extensionBuilder(aClassic)
                .addCapability("a")
                .build());

        final ArtifactCoords aReactive = ArtifactCoords.fromString("org.acme:a-reactive:1.0");
        catalog.addExtension(extensionBuilder(aReactive)
                .addCapability("a")
                .build());

        catalog.addExtension(new ExtensionBuilder(ArtifactCoords.fromString("org.acme:b-classic:1.0"))
                .addCapability("classic")
                .build());

        catalog.addExtension(new ExtensionBuilder(ArtifactCoords.fromString("org.acme:b-reactive:1.0"))
                .addCapability("reactive")
                .build());

        final Map<ArtifactCoords, ExtensionCompatibility> compatMap = compatMap(catalog);
        assertFalse(compatMap.isEmpty());
        assertIncompatibleWith(compatMap.get(aClassic), aReactive);
        assertIncompatibleWith(compatMap.get(aReactive), aClassic);
        assertEquals(2, compatMap.size());
    }

    @Test
    public void testTransitiveCapabilityConflict() throws Exception {

        final JsonExtensionCatalog catalog = new JsonExtensionCatalog();

        final ArtifactCoords aClassic = ArtifactCoords.fromString("org.acme:a-classic:1.0");
        catalog.addExtension(extensionBuilder(aClassic)
                .addCapability("a")
                .build());

        final ArtifactCoords aReactive = ArtifactCoords.fromString("org.acme:a-reactive:1.0");
        catalog.addExtension(extensionBuilder(aReactive)
                .addCapability("a")
                .build());

        final ArtifactCoords bClassic = ArtifactCoords.fromString("org.acme:b-classic:1.0");
        catalog.addExtension(new ExtensionBuilder(bClassic)
                .addDependency(aClassic)
                .build());

        final ArtifactCoords bReactive = ArtifactCoords.fromString("org.acme:b-reactive:1.0");
        catalog.addExtension(new ExtensionBuilder(bReactive)
                .addDependency(aReactive)
                .build());

        final ArtifactCoords cClassic = ArtifactCoords.fromString("org.acme:c-classic:1.0");
        catalog.addExtension(new ExtensionBuilder(cClassic)
                .addDependency(bClassic)
                .addDependency(aClassic)
                .build());

        final ArtifactCoords cReactive = ArtifactCoords.fromString("org.acme:c-reactive:1.0");
        catalog.addExtension(new ExtensionBuilder(cReactive)
                .addDependency(bReactive)
                .addDependency(aReactive)
                .build());

        final Map<ArtifactCoords, ExtensionCompatibility> compatMap = compatMap(catalog);
        assertFalse(compatMap.isEmpty());
        assertIncompatibleWith(compatMap.get(aClassic), aReactive, bReactive, cReactive);
        assertIncompatibleWith(compatMap.get(aReactive), aClassic, bClassic, cClassic);
        assertIncompatibleWith(compatMap.get(bClassic), aReactive, bReactive, cReactive);
        assertIncompatibleWith(compatMap.get(bReactive), aClassic, bClassic, cClassic);
        assertIncompatibleWith(compatMap.get(cClassic), aReactive, bReactive, cReactive);
        assertIncompatibleWith(compatMap.get(cReactive), aClassic, bClassic, cClassic);
        assertEquals(6, compatMap.size());
    }

    private static void assertIncompatibleWith(ExtensionCompatibility ec, ArtifactCoords... keys) {
        assertNotNull(ec);
        assertEquals(new HashSet<>(Arrays.asList(keys)), coordsSet(ec.getIncompatibleExtensions()));
    }

    private static Set<ArtifactCoords> coordsSet(Collection<Extension> extensions) {
        return extensions.stream().map(e -> e.getArtifact()).collect(Collectors.toSet());
    }

    private static Map<ArtifactCoords, ExtensionCompatibility> compatMap(ExtensionCatalog catalog) {
        return toMap(ExtensionCatalogCompatibility.forCatalog(catalog).getExtensionCompatibility());
    }

    private static Map<ArtifactCoords, ExtensionCompatibility> toMap(Collection<ExtensionCompatibility> c) {
        final Map<ArtifactCoords, ExtensionCompatibility> m = new HashMap<>(c.size());
        c.forEach(e -> m.put(e.getExtension().getArtifact(), e));
        return m;
    }

    private static ExtensionBuilder extensionBuilder(ArtifactCoords coords) {
        return new ExtensionBuilder(coords);
    }

    private static class ExtensionBuilder {

        private final JsonExtension e = new JsonExtension();

        ExtensionBuilder(ArtifactCoords coords) {
            e.setArtifact(coords);
        }

        @SuppressWarnings("unchecked")
        ExtensionBuilder addCapability(String cap) {
            ((Map<String, List<String>>) e.getMetadata().computeIfAbsent("capabilities",
                    s -> Collections.singletonMap("provides", new ArrayList<>()))).get("provides").add(cap);
            return this;
        }

        @SuppressWarnings("unchecked")
        ExtensionBuilder addDependency(ArtifactCoords coords) {
            ((List<String>) e.getMetadata().computeIfAbsent("extension-dependencies",
                    s -> new ArrayList<>())).add(coords.getKey().toString());
            return this;
        }

        Extension build() {
            return e;
        }
    }
}
