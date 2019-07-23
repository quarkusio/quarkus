package io.quarkus.deployment.index;

import static io.quarkus.deployment.index.ApplicationArchiveBuildStep.urlToPath;
import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;

public class ApplicationArchiveBuildStepTestCase {
    @Test
    public void testUrlToPath() throws MalformedURLException {
        assertEquals(Paths.get("/a/path"), urlToPath(new URL("jar:file:/a/path!/META-INF/services/my.Service"), "META-INF"));
        assertEquals(Paths.get("/a/path with whitespace"),
                urlToPath(new URL("jar:file:/a/path%20with%20whitespace!/META-INF/services/my.Service"), "META-INF"));
        assertEquals(Paths.get("/a/path"), urlToPath(new URL("file:/a/path/META-INF/services/my.Service"), "META-INF"));
        assertEquals(Paths.get("/a/path with whitespace"),
                urlToPath(new URL("file:/a/path%20with%20whitespace/META-INF/services/my.Service"), "META-INF"));
        assertEquals(Paths.get("/a/path"), urlToPath(new URL("file:/a/path"), ""));
        assertEquals(Paths.get("/a/path with whitespace"), urlToPath(new URL("file:/a/path%20with%20whitespace"), ""));
    }

    @Test(expected = RuntimeException.class)
    public void testUrlToPathWithWrongProtocol() throws MalformedURLException {
        urlToPath(new URL("http://a/path"), "");
    }
}
