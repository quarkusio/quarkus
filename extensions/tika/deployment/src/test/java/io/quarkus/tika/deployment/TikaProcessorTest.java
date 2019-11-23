package io.quarkus.tika.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class TikaProcessorTest {

    // We must register a configuration otherwise we'll get an exception.

    static volatile SmallRyeConfig config;

    @BeforeAll
    public static void setItUp() {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.addDefaultSources();
        builder.addDiscoveredConverters();
        builder.addDiscoveredSources();
        config = builder.build();
        QuarkusConfigFactory.setConfig(config);
        ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        final Config existingConfig = cpr.getConfig();
        if (existingConfig != TikaProcessorTest.config) {
            cpr.releaseConfig(existingConfig);
        }
    }

    @AfterAll
    public static void tearItDown() {
        ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        cpr.releaseConfig(config);
    }

    @Test
    public void testSupportedParserNames() throws Exception {
        Optional<String> parserNames = Optional.of("pdf");
        List<String> names = TikaProcessor.getSupportedParserNames(parserNames);
        assertEquals(1, names.size());
        assertEquals("org.apache.tika.parser.pdf.PDFParser", names.get(0));
    }

    @Test
    public void testResolvableCustomAbbreviation() throws Exception {
        Optional<String> parserNames = Optional.of("pdf,opendoc");
        List<String> names = TikaProcessor.getSupportedParserNames(parserNames);
        assertEquals(2, names.size());
        assertTrue(names.contains("org.apache.tika.parser.pdf.PDFParser"));
        assertTrue(names.contains("org.apache.tika.parser.odf.OpenDocumentParser"));
    }

    @Test
    public void testUnresolvableCustomAbbreviation() throws Exception {
        Optional<String> parserNames = Optional.of("classparser");
        try {
            TikaProcessor.getSupportedParserNames(parserNames);
            fail("'classparser' is not resolvable");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    @Test
    public void testAllSupportedParserNames() throws Exception {
        Optional<String> parserNames = Optional.ofNullable(null);
        List<String> names = TikaProcessor.getSupportedParserNames(parserNames);
        assertEquals(68, names.size());
    }
}
