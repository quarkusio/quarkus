package io.quarkus.compressors.it;

import static io.quarkus.compressors.it.Testflow.runDecompressorsTest;

import java.net.URL;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RESTEndpointsTest {

    @TestHTTPResource(value = "/decompressed")
    URL url;

    @ParameterizedTest
    @CsvSource(value = {
    //@formatter:off
    // Context  | Accept-Encoding | Content-Encoding | Method
    "/text     | identity         | br               | POST",
    "/text     | identity         | br               | PUT",
    "/text     | identity         | gzip             | POST",
    "/json     | identity         | br               | POST",
    //@formatter:on
    }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, nullValues = "null")
    public void testDecompressors(String endpoint, String acceptEncoding, String contentEncoding, String method) {
        runDecompressorsTest(url.toString() + endpoint, acceptEncoding, contentEncoding, method);
    }
}
