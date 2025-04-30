package io.quarkus.compressors.it;

import static io.quarkus.compressors.it.Testflow.runCompressorsTest;
import static io.quarkus.compressors.it.Testflow.runDecompressorsTest;

import java.net.URL;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RESTEndpointsTest {

    @TestHTTPResource(value = "/compressed")
    URL urlCompressed;

    @TestHTTPResource(value = "/decompressed")
    URL urlDEcompressed;

    @ParameterizedTest
    @CsvSource(value = {
    //@formatter:off
    // Context | Accept-Encoding | Content-Encoding | Content-Length
    "/yes/text | deflate,gzip,br | br               | 2316",
    "/yes/text | deflate         | deflate          | 2402",
    "/no/text  | deflate,gzip,br | null             | 6483",
    "/yes/json | deflate         | deflate          | 2402",
    "/no/json  | deflate,gzip,br | null             | 6483",
    "/yes/xml  | deflate,gzip,br | br               | 2316",
    "/no/xml   | deflate,gzip,br | null             | 6483",
    "/yes/xhtml| deflate,gzip    | gzip             | 2414",
    //@formatter:on
    }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, nullValues = "null")
    public void testCompressors(String endpoint, String acceptEncoding, String contentEncoding, String contentLength) {
        runCompressorsTest(urlCompressed.toString() + endpoint, acceptEncoding, contentEncoding, contentLength);
    }

    @ParameterizedTest
    @CsvSource(value = {
    //@formatter:off
    // Context  | Accept-Encoding        | Content-Encoding | Method
    "/text     | identity                | br               | POST",
    "/text     | identity                | gzip             | POST",
    "/text     | identity                | deflate          | POST",
    "/text     | identity                | br               | PUT",
    "/text     | identity                | gzip             | PUT",
    "/text     | identity                | deflate          | PUT",
    "/text     | deflate                 | br               | POST",
    "/text     | deflate                 | gzip             | POST",
    "/text     | deflate                 | deflate          | POST",
    "/text     | gzip                    | br               | PUT",
    "/text     | gzip                    | gzip             | PUT",
    "/text     | gzip                    | deflate          | PUT",
    "/text     | br                      | br               | POST",
    "/text     | br                      | gzip             | POST",
    "/text     | br                      | deflate          | POST",
    "/text     | br                      | br               | PUT",
    "/text     | br                      | gzip             | PUT",
    "/text     | gzip,br,deflate         | deflate          | PUT",
    "/json     | identity                | br               | POST",
    "/json     | identity                | gzip             | POST",
    "/json     | identity                | deflate          | POST",
    "/json     | identity                | br               | PUT",
    "/json     | identity                | gzip             | PUT",
    "/json     | identity                | deflate          | PUT",
    "/json     | deflate                 | br               | POST",
    "/json     | deflate                 | gzip             | POST",
    "/json     | deflate                 | deflate          | POST",
    "/json     | gzip                    | br               | PUT",
    "/json     | gzip                    | gzip             | PUT",
    "/json     | gzip                    | deflate          | PUT",
    "/json     | br                      | br               | POST",
    "/json     | br                      | gzip             | POST",
    "/json     | br                      | deflate          | POST",
    "/json     | br                      | br               | PUT",
    "/json     | br                      | gzip             | PUT",
    "/json     | gzip,br,deflate         | deflate          | PUT",
    "/xml      | identity                | br               | POST",
    "/xml      | identity                | gzip             | POST",
    "/xml      | identity                | deflate          | POST",
    "/xml      | identity                | br               | PUT",
    "/xml      | identity                | gzip             | PUT",
    "/xml      | identity                | deflate          | PUT",
    "/xml      | deflate                 | br               | POST",
    "/xml      | deflate                 | gzip             | POST",
    "/xml      | deflate                 | deflate          | POST",
    "/xml      | gzip                    | br               | PUT",
    "/xml      | gzip                    | gzip             | PUT",
    "/xml      | gzip                    | deflate          | PUT",
    "/xml      | br                      | br               | POST",
    "/xml      | br                      | gzip             | POST",
    "/xml      | br                      | deflate          | POST",
    "/xml      | br                      | br               | PUT",
    "/xml      | br                      | gzip             | PUT",
    "/xml      | gzip,br,deflate         | deflate          | PUT",
    "/xhtml    | identity                | br               | POST",
    "/xhtml    | identity                | gzip             | POST",
    "/xhtml    | identity                | deflate          | POST",
    "/xhtml    | identity                | br               | PUT",
    "/xhtml    | identity                | gzip             | PUT",
    "/xhtml    | identity                | deflate          | PUT",
    "/xhtml    | deflate                 | br               | POST",
    "/xhtml    | deflate                 | gzip             | POST",
    "/xhtml    | deflate                 | deflate          | POST",
    "/xhtml    | gzip                    | br               | PUT",
    "/xhtml    | gzip                    | gzip             | PUT",
    "/xhtml    | gzip                    | deflate          | PUT",
    "/xhtml    | br                      | br               | POST",
    "/xhtml    | br                      | gzip             | POST",
    "/xhtml    | br                      | deflate          | POST",
    "/xhtml    | br                      | br               | PUT",
    "/xhtml    | br                      | gzip             | PUT",
    "/xhtml    | gzip,br,deflate         | deflate          | PUT"
    //@formatter:on
    }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, nullValues = "null")
    public void testDecompressors(String endpoint, String acceptEncoding, String contentEncoding, String method) {
        runDecompressorsTest(urlDEcompressed.toString() + endpoint, acceptEncoding, contentEncoding, method);
    }
}
