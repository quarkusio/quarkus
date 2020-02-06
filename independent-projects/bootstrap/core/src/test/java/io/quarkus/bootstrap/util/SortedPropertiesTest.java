package io.quarkus.bootstrap.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SortedPropertiesTest {

    @Test
    public void store() throws IOException {
        assertStore(null, "k1", "v1", "k2", "v2", "k1=v1\nk2=v2\n");
        assertStore("", "k1", "v1", "k2", "v2", "#\nk1=v1\nk2=v2\n");
        assertStore("\n", "k1", "v1", "k2", "v2", "#\n#\nk1=v1\nk2=v2\n");
        assertStore("\r\n", "k1", "v1", "k2", "v2", "#\n#\nk1=v1\nk2=v2\n");
        assertStore("cmt", "k1", "v1", "k2", "v2", "#cmt\nk1=v1\nk2=v2\n");
        assertStore("cmt\n", "k1", "v1", "k2", "v2", "#cmt\n#\nk1=v1\nk2=v2\n");
        assertStore("cmt\r\n", "k1", "v1", "k2", "v2", "#cmt\n#\nk1=v1\nk2=v2\n");
    }

    static void assertStore(String comments, String k1, String v1, String k2, String v2, String expected) throws IOException {
        final Properties props = new SortedProperties();
        props.setProperty(k2, v2);
        props.setProperty(k1, v1);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            props.store(baos, comments);
            final String actual = new String(baos.toByteArray(), StandardCharsets.ISO_8859_1);
            Assertions.assertEquals(expected, actual);
        }

        try (StringWriter w = new StringWriter()) {
            props.store(w, comments);
            final String actual = w.toString();
            Assertions.assertEquals(expected, actual);
        }

    }

}
