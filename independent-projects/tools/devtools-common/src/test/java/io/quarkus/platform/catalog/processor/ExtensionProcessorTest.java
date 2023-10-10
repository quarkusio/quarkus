package io.quarkus.platform.catalog.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.registry.catalog.Extension;

public class ExtensionProcessorTest {

    @Test
    public void testNoSupportData() throws Exception {
        Extension extension = Extension
                .fromFile(Path.of(ExtensionProcessorTest.class.getResource("/resteasy-extension.yaml").toURI()));
        Map<String, Collection<String>> metadata = ExtensionProcessor.getSyntheticMetadata(extension);

        assertFalse(metadata.keySet().contains("support"));
    }

    @Test
    public void testFullSupportData() throws Exception {
        Extension extension = Extension
                .fromFile(Path.of(ExtensionProcessorTest.class.getResource("/rest-client-mutiny-extension.yaml").toURI()));
        Map<String, Collection<String>> metadata = ExtensionProcessor.getSyntheticMetadata(extension);

        assertTrue(metadata.keySet().contains("support"));
        String supporter = metadata.get("support").iterator().next();
        assertTrue(supporter.contains("id=xyz"));
        assertTrue(supporter.contains("status=[techpreview, deprecated]"));
    }

    @Test
    public void testLegacySupportData() throws Exception {
        Extension extension = Extension
                .fromFile(Path.of(ExtensionProcessorTest.class.getResource("/agroal-extension.yaml").toURI()));
        Map<String, Collection<String>> metadata = ExtensionProcessor.getSyntheticMetadata(extension);

        assertTrue(metadata.keySet().contains("support"));
        String supporter = metadata.get("support").iterator().next();
        assertTrue(supporter.contains("id=redhat"));
        assertTrue(supporter.contains("status=[stable]"));
    }

    @Test
    public void testComplexSupportData() throws Exception {
        Extension extension = Extension
                .fromFile(Path.of(ExtensionProcessorTest.class.getResource("/datasource-extension.yaml").toURI()));
        Map<String, Collection<String>> metadata = ExtensionProcessor.getSyntheticMetadata(extension);

        assertTrue(metadata.keySet().contains("support"));
        Collection<String> supporters = metadata.get("support");
        assertEquals(2, supporters.size());
        Iterator<String> it = supporters.iterator();
        String supporter1 = it.next();
        String supporter2 = it.next();
        assertTrue(supporter1.contains("name=Red Hat"));
        assertTrue(supporter1.contains("status=[stable, awesome]"));
        assertTrue(supporter2.contains("id=xyz"));
        assertTrue(supporter2.contains("status=[none]"));
    }

    @Test
    public void testSupporterWithoutStatus() throws Exception {
        Extension extension = Extension
                .fromFile(Path.of(ExtensionProcessorTest.class.getResource("/flyway-extension.yaml").toURI()));
        Map<String, Collection<String>> metadata = ExtensionProcessor.getSyntheticMetadata(extension);

        assertTrue(metadata.keySet().contains("support"));
        Collection<String> supporters = metadata.get("support");
        assertEquals(2, supporters.size());
        Iterator<String> it = supporters.iterator();
        String supporter1 = it.next();
        String supporter2 = it.next();
        assertTrue(supporter1.contains("id=xyz"));
        assertTrue(supporter1.contains("status=[stable]"));
        assertTrue(supporter2.contains("id=abc"));
        assertTrue(supporter2.contains("status=[stable]"));
    }

}
