package io.quarkus.it.resteasy.jackson;

import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(StaticContentWithTestProfileTest.DummyProfile.class)
public class StaticContentWithTestProfileTest {

    @TestHTTPResource("index.html")
    URL url;

    @Test
    public void testIndexHtml() throws Exception {
        try (InputStream in = url.openStream()) {
            String contents = StaticContentTest.readStream(in);
            Assertions.assertTrue(contents.contains("<title>Testing Guide</title>"));
        }
    }

    public static class DummyProfile implements QuarkusTestProfile {

    }
}
