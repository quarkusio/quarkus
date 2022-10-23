package io.quarkus.platform.catalog.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.registry.catalog.Extension;

public class ExtensionProcessorTest {

    @Test
    public void testNoSupportData() throws Exception {
        Extension extension = Extension
                .fromFile(Path.of(ExtensionProcessorTest.class.getResource("/resteasy-extension.yaml").toURI()));
        Map<String, Collection<String>> metadata = ExtensionProcessor.getSyntheticMetadata(extension);

        assertFalse(metadata.keySet().contains("supported-by"));
        assertFalse(metadata.keySet().stream().anyMatch((k) -> k.endsWith("-support")));
    }

    @Test
    public void testFullSupportData() throws Exception {
        Extension extension = Extension
                .fromFile(Path.of(ExtensionProcessorTest.class.getResource("/rest-client-mutiny-extension.yaml").toURI()));
        Map<String, Collection<String>> metadata = ExtensionProcessor.getSyntheticMetadata(extension);

        assertEquals(Arrays.asList("xyz"), metadata.get("supported-by"));
        assertEquals(Arrays.asList("techpreview", "deprecated"), metadata.get("xyz-support"));
    }

    @Test
    public void testLegacySupportData() throws Exception {
        Extension extension = Extension
                .fromFile(Path.of(ExtensionProcessorTest.class.getResource("/agroal-extension.yaml").toURI()));
        Map<String, Collection<String>> metadata = ExtensionProcessor.getSyntheticMetadata(extension);

        assertEquals(Arrays.asList("redhat"), metadata.get("supported-by"));
        assertEquals(Arrays.asList("stable"), metadata.get("redhat-support"));
    }

    @Test
    public void testComplexSupportData() throws Exception {
        Extension extension = Extension
                .fromFile(Path.of(ExtensionProcessorTest.class.getResource("/datasource-extension.yaml").toURI()));
        Map<String, Collection<String>> metadata = ExtensionProcessor.getSyntheticMetadata(extension);

        assertEquals(Arrays.asList("Red Hat", "xyz"), metadata.get("supported-by"));
        assertEquals(Arrays.asList("stable", "awesome"), metadata.get("Red Hat-support"));
        assertEquals(Arrays.asList("none"), metadata.get("xyz-support"));
    }

    @Test
    public void testSupporterWithoutStatus() throws Exception {
        Extension extension = Extension
                .fromFile(Path.of(ExtensionProcessorTest.class.getResource("/flyway-extension.yaml").toURI()));
        Map<String, Collection<String>> metadata = ExtensionProcessor.getSyntheticMetadata(extension);

        assertEquals(Arrays.asList("xyz", "abc"), metadata.get("supported-by"));
        assertEquals(Arrays.asList("stable"), metadata.get("xyz-support"));
        assertEquals(Arrays.asList("stable"), metadata.get("abc-support"));
    }

}
