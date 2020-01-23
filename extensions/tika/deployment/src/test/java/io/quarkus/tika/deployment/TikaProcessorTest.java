package io.quarkus.tika.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TikaProcessorTest {

    @RegisterExtension
    static final QuarkusUnitTest quarkusUnitTest = new QuarkusUnitTest();

    @Test
    public void testPDFParserName() throws Exception {
        Set<String> names = getParserNames(null, "pdf");
        assertEquals(1, names.size());
        assertTrue(names.contains("org.apache.tika.parser.pdf.PDFParser"));
    }

    @Test
    public void testODFParserName() throws Exception {
        Set<String> names = getParserNames(null, "odf");
        assertEquals(1, names.size());
        assertTrue(names.contains("org.apache.tika.parser.odf.OpenDocumentParser"));
    }

    @Test
    public void testSupportedParserNames() throws Exception {
        Set<String> names = getParserNames(null, "pdf,odf");
        assertEquals(2, names.size());
        assertTrue(names.contains("org.apache.tika.parser.pdf.PDFParser"));
        assertTrue(names.contains("org.apache.tika.parser.odf.OpenDocumentParser"));
    }

    @Test
    public void testResolvableCustomAbbreviation() throws Exception {
        Set<String> names = getParserConfig(null, "pdf,opendoc", Collections.emptyMap(),
                Collections.singletonMap("opendoc",
                        "org.apache.tika.parser.odf.OpenDocumentParser")).keySet();
        assertEquals(2, names.size());
        assertTrue(names.contains("org.apache.tika.parser.pdf.PDFParser"));
        assertTrue(names.contains("org.apache.tika.parser.odf.OpenDocumentParser"));
    }

    @Test
    public void testPdfParserConfig() throws Exception {
        Map<String, List<TikaProcessor.TikaParserParameter>> parserConfig = getParserConfig(null, "pdf",
                Collections.singletonMap("pdf",
                        Collections.singletonMap("sort-by-position", "true")),
                Collections.emptyMap());
        assertEquals(1, parserConfig.size());

        String pdfParserFullName = "org.apache.tika.parser.pdf.PDFParser";
        assertEquals(1, parserConfig.get(pdfParserFullName).size());
        assertEquals("sortByPosition", parserConfig.get(pdfParserFullName).get(0).getName());
        assertEquals("true", parserConfig.get(pdfParserFullName).get(0).getValue());
    }

    @Test
    public void testUnresolvableCustomAbbreviation() throws Exception {
        try {
            getParserNames(null, "classparser");
            fail("'classparser' is not resolvable");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    @Test
    public void testAllSupportedParserNames() throws Exception {
        assertEquals(69, getParserNames(null, null).size());
    }

    @Test
    public void testSupportedParserNamesWithTikaConfigPath() throws Exception {
        Set<String> names = getParserNames("tika-config.xml", "pdf");
        assertEquals(69, names.size());
    }

    @Test
    public void testUnhyphenation() {
        assertEquals("sortByPosition", TikaProcessor.unhyphenate("sort-by-position"));
        assertEquals("position", TikaProcessor.unhyphenate("position"));
    }

    private Set<String> getParserNames(String tikaConfigPath, String parsers) throws Exception {
        return TikaProcessor.getSupportedParserConfig(
                Optional.ofNullable(tikaConfigPath), Optional.ofNullable(parsers),
                Collections.emptyMap(), Collections.emptyMap()).keySet();
    }

    private Map<String, List<TikaProcessor.TikaParserParameter>> getParserConfig(String tikaConfigPath, String parsers,
            Map<String, Map<String, String>> parserParamMaps,
            Map<String, String> parserAbbreviations) throws Exception {
        return TikaProcessor.getSupportedParserConfig(
                Optional.ofNullable(tikaConfigPath), Optional.ofNullable(parsers),
                parserParamMaps, parserAbbreviations);
    }
}
