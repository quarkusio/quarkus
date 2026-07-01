package org.jboss.resteasy.reactive.common.headers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;
import org.junit.jupiter.api.Test;

class HeaderUtilTest {

    @Test
    void getAcceptableLanguages() {
        MultivaluedTreeMap<String, String> emptyHeaders = new MultivaluedTreeMap<>();
        Locale[] wildcardLocale = new Locale[] { new Locale("*") };
        assertArrayEquals(wildcardLocale, HeaderUtil.getAcceptableLanguages(emptyHeaders).toArray());

        MultivaluedTreeMap<String, String> singleLanguage = new MultivaluedTreeMap<>();
        singleLanguage.add("Accept-Language", "de");
        Locale[] singleLocale = new Locale[] { Locale.GERMAN };
        assertArrayEquals(singleLocale, HeaderUtil.getAcceptableLanguages(singleLanguage).toArray());

        MultivaluedTreeMap<String, String> multipleWeightedLanguages = new MultivaluedTreeMap<>();
        multipleWeightedLanguages.add("Accept-Language", "da, en-gb;q=0.8, en;q=0.7");
        Locale[] multipleWeightedLocales = new Locale[] { new Locale("da"), Locale.UK, Locale.ENGLISH };
        assertArrayEquals(multipleWeightedLocales, HeaderUtil.getAcceptableLanguages(multipleWeightedLanguages).toArray());
    }

    @Test
    void sanitizeFileNamePassesNormalFilenames() {
        assertEquals("report.pdf", HeaderUtil.sanitizeFileName("report.pdf"));
        assertEquals("my file (1).txt", HeaderUtil.sanitizeFileName("my file (1).txt"));
        assertEquals(".hidden", HeaderUtil.sanitizeFileName(".hidden"));
    }

    @Test
    void sanitizeFileNameStripsForwardSlashTraversal() {
        assertEquals("pwned.txt", HeaderUtil.sanitizeFileName("../../../tmp/pwned.txt"));
        assertEquals("pwned.txt", HeaderUtil.sanitizeFileName("/tmp/pwned.txt"));
        assertEquals("pwned.txt", HeaderUtil.sanitizeFileName("a/b/c/pwned.txt"));
    }

    @Test
    void sanitizeFileNameStripsBackslashTraversal() {
        assertEquals("pwned.txt", HeaderUtil.sanitizeFileName("..\\..\\..\\tmp\\pwned.txt"));
        assertEquals("pwned.txt", HeaderUtil.sanitizeFileName("C:\\Users\\bad\\pwned.txt"));
    }

    @Test
    void sanitizeFileNameStripsMixedSeparators() {
        assertEquals("pwned.txt", HeaderUtil.sanitizeFileName("../..\\../tmp/pwned.txt"));
    }

    @Test
    void sanitizeFileNameStripsNullBytes() {
        assertEquals("shell.jsp.txt", HeaderUtil.sanitizeFileName("shell.jsp\0.txt"));
        assertEquals("file.txt", HeaderUtil.sanitizeFileName("file\0.txt"));
    }

    @Test
    void sanitizeFileNameRejectsDotAndDotDot() {
        assertNull(HeaderUtil.sanitizeFileName("."));
        assertNull(HeaderUtil.sanitizeFileName(".."));
        assertNull(HeaderUtil.sanitizeFileName("foo/.."));
        assertNull(HeaderUtil.sanitizeFileName("foo/."));
    }

    @Test
    void sanitizeFileNameRejectsEmptyAndNull() {
        assertNull(HeaderUtil.sanitizeFileName(null));
        assertNull(HeaderUtil.sanitizeFileName(""));
        assertNull(HeaderUtil.sanitizeFileName("/"));
        assertNull(HeaderUtil.sanitizeFileName("\\"));
        assertNull(HeaderUtil.sanitizeFileName("///"));
    }

    @Test
    void sanitizeFileNameHandlesNullByteInPath() {
        assertEquals("pwned.txt", HeaderUtil.sanitizeFileName("../\0../pwned.txt"));
    }

    @Test
    void sanitizeFileNamePreservesUnicodeFilenames() {
        assertEquals("文件.txt", HeaderUtil.sanitizeFileName("文件.txt"));
        assertEquals("café.pdf", HeaderUtil.sanitizeFileName("café.pdf"));
        assertEquals("文件.txt", HeaderUtil.sanitizeFileName("path/to/文件.txt"));
    }

    @Test
    void sanitizeFileNameWithExtractQuotedValueFromHeaderWithEncoding() {
        String header = "form-data; name=\"file\"; filename*=UTF-8''..%2F..%2Fpwned.txt";
        String extracted = HeaderUtil.extractQuotedValueFromHeaderWithEncoding(header, "filename");
        String sanitized = HeaderUtil.sanitizeFileName(extracted);
        assertEquals("pwned.txt", sanitized);
    }
}
