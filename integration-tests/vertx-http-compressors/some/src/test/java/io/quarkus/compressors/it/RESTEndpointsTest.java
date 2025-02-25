package io.quarkus.compressors.it;

import static io.quarkus.compressors.it.Testflow.runCompressorsTest;

import java.net.URL;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RESTEndpointsTest {

    @TestHTTPResource(value = "/compressed")
    URL url;

    @ParameterizedTest
    @CsvSource(value = {
    //@formatter:off
    // Context | Accept-Encoding | Content-Encoding | Content-Length
    "/yes/text | deflate,gzip,br | gzip             | 2414",
    "/yes/text | deflate         | deflate          | 2402",
    "/no/text  | deflate,gzip,br | null             | 6483",
    "/yes/json | deflate         | deflate          | 2402",
    "/no/json  | deflate,gzip,br | null             | 6483",
    "/yes/xml  | deflate,gzip,br | gzip             | 2414",
    "/no/xml   | deflate,gzip,br | null             | 6483",
    //@formatter:on
    }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, nullValues = "null")
    public void testCompressors(String endpoint, String acceptEncoding, String contentEncoding, String contentLength) {
        runCompressorsTest(url.toString() + endpoint, acceptEncoding, contentEncoding, contentLength);
    }
}
