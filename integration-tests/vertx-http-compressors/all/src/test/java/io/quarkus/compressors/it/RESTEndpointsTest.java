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
//    "/yes/text | deflate,gzip,br | br               | 2316", // Skip Brotli as we cannot configure the quality - https://github.com/eclipse-vertx/vert.x/issues/6201
    "/yes/text | deflate         | deflate          | 2402",
    "/no/text  | deflate,gzip,br | null             | 6483",
    "/yes/json | deflate         | deflate          | 2402",
    "/no/json  | deflate,gzip,br | null             | 6483",
//    "/yes/xml  | deflate,gzip,br | br               | 2316", // Skip Brotli as we cannot configure the quality - https://github.com/eclipse-vertx/vert.x/issues/6201
    "/no/xml   | deflate,gzip,br | null             | 6483",
    "/yes/xhtml| deflate,gzip    | gzip             | 2414",
    "/yes/text | snappy          | snappy           | 3279",
    "/yes/json | snappy          | snappy           | 3279",
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
    "/text     | identity                | snappy           | POST",
    "/text     | identity                | br               | PUT",
    "/text     | identity                | gzip             | PUT",
    "/text     | identity                | deflate          | PUT",
    "/text     | identity                | snappy           | PUT",
    "/text     | deflate                 | br               | POST",
    "/text     | deflate                 | gzip             | POST",
    "/text     | deflate                 | deflate          | POST",
    "/text     | deflate                 | snappy           | POST",
    "/text     | gzip                    | br               | PUT",
    "/text     | gzip                    | gzip             | PUT",
    "/text     | gzip                    | deflate          | PUT",
    "/text     | gzip                    | snappy           | PUT",
    "/text     | br                      | br               | POST",
    "/text     | br                      | gzip             | POST",
    "/text     | br                      | deflate          | POST",
    "/text     | br                      | snappy           | POST",
    "/text     | br                      | br               | PUT",
    "/text     | br                      | gzip             | PUT",
    "/text     | br                      | snappy           | PUT",
    "/text     | snappy                  | br               | POST",
    "/text     | snappy                  | gzip             | POST",
    "/text     | snappy                  | deflate          | POST",
    "/text     | snappy                  | snappy           | POST",
    "/text     | snappy                  | br               | PUT",
    "/text     | snappy                  | gzip             | PUT",
    "/text     | snappy                  | snappy           | PUT",
    "/text     | gzip,br,deflate,snappy  | deflate          | PUT",
    "/text     | gzip,br,deflate,snappy  | snappy           | PUT",
    "/json     | identity                | br               | POST",
    "/json     | identity                | gzip             | POST",
    "/json     | identity                | deflate          | POST",
    "/json     | identity                | snappy           | POST",
    "/json     | identity                | br               | PUT",
    "/json     | identity                | gzip             | PUT",
    "/json     | identity                | deflate          | PUT",
    "/json     | identity                | snappy           | PUT",
    "/json     | deflate                 | br               | POST",
    "/json     | deflate                 | gzip             | POST",
    "/json     | deflate                 | deflate          | POST",
    "/json     | deflate                 | snappy           | POST",
    "/json     | gzip                    | br               | PUT",
    "/json     | gzip                    | gzip             | PUT",
    "/json     | gzip                    | deflate          | PUT",
    "/json     | gzip                    | snappy           | PUT",
    "/json     | br                      | br               | POST",
    "/json     | br                      | gzip             | POST",
    "/json     | br                      | deflate          | POST",
    "/json     | br                      | snappy           | POST",
    "/json     | br                      | br               | PUT",
    "/json     | br                      | gzip             | PUT",
    "/json     | br                      | snappy           | PUT",
    "/json     | snappy                  | br               | POST",
    "/json     | snappy                  | gzip             | POST",
    "/json     | snappy                  | deflate          | POST",
    "/json     | snappy                  | snappy           | POST",
    "/json     | snappy                  | br               | PUT",
    "/json     | snappy                  | gzip             | PUT",
    "/json     | snappy                  | snappy           | PUT",
    "/json     | gzip,br,deflate,snappy  | deflate          | PUT",
    "/json     | gzip,br,deflate,snappy  | snappy           | PUT",
    "/xml      | identity                | br               | POST",
    "/xml      | identity                | gzip             | POST",
    "/xml      | identity                | deflate          | POST",
    "/xml      | identity                | snappy           | POST",
    "/xml      | identity                | br               | PUT",
    "/xml      | identity                | gzip             | PUT",
    "/xml      | identity                | deflate          | PUT",
    "/xml      | identity                | snappy           | PUT",
    "/xml      | deflate                 | br               | POST",
    "/xml      | deflate                 | gzip             | POST",
    "/xml      | deflate                 | deflate          | POST",
    "/xml      | deflate                 | snappy           | POST",
    "/xml      | gzip                    | br               | PUT",
    "/xml      | gzip                    | gzip             | PUT",
    "/xml      | gzip                    | deflate          | PUT",
    "/xml      | gzip                    | snappy           | PUT",
    "/xml      | br                      | br               | POST",
    "/xml      | br                      | gzip             | POST",
    "/xml      | br                      | deflate          | POST",
    "/xml      | br                      | snappy           | POST",
    "/xml      | br                      | br               | PUT",
    "/xml      | br                      | gzip             | PUT",
    "/xml      | br                      | snappy           | PUT",
    "/xml      | snappy                  | br               | POST",
    "/xml      | snappy                  | gzip             | POST",
    "/xml      | snappy                  | deflate          | POST",
    "/xml      | snappy                  | snappy           | POST",
    "/xml      | snappy                  | br               | PUT",
    "/xml      | snappy                  | gzip             | PUT",
    "/xml      | snappy                  | snappy           | PUT",
    "/xml      | gzip,br,deflate,snappy  | deflate          | PUT",
    "/xml      | gzip,br,deflate,snappy  | snappy           | PUT",
    "/xhtml    | identity                | br               | POST",
    "/xhtml    | identity                | gzip             | POST",
    "/xhtml    | identity                | deflate          | POST",
    "/xhtml    | identity                | snappy           | POST",
    "/xhtml    | identity                | br               | PUT",
    "/xhtml    | identity                | gzip             | PUT",
    "/xhtml    | identity                | deflate          | PUT",
    "/xhtml    | identity                | snappy           | PUT",
    "/xhtml    | deflate                 | br               | POST",
    "/xhtml    | deflate                 | gzip             | POST",
    "/xhtml    | deflate                 | deflate          | POST",
    "/xhtml    | deflate                 | snappy           | POST",
    "/xhtml    | gzip                    | br               | PUT",
    "/xhtml    | gzip                    | gzip             | PUT",
    "/xhtml    | gzip                    | deflate          | PUT",
    "/xhtml    | gzip                    | snappy           | PUT",
    "/xhtml    | br                      | br               | POST",
    "/xhtml    | br                      | gzip             | POST",
    "/xhtml    | br                      | deflate          | POST",
    "/xhtml    | br                      | snappy           | POST",
    "/xhtml    | br                      | br               | PUT",
    "/xhtml    | br                      | gzip             | PUT",
    "/xhtml    | br                      | snappy           | PUT",
    "/xhtml    | snappy                  | br               | POST",
    "/xhtml    | snappy                  | gzip             | POST",
    "/xhtml    | snappy                  | deflate          | POST",
    "/xhtml    | snappy                  | snappy           | POST",
    "/xhtml    | snappy                  | br               | PUT",
    "/xhtml    | snappy                  | gzip             | PUT",
    "/xhtml    | snappy                  | snappy           | PUT",
    "/xhtml    | gzip,br,deflate,snappy  | deflate          | PUT",
    "/xhtml    | gzip,br,deflate,snappy  | snappy           | PUT"
    //@formatter:on
    }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = true, nullValues = "null")
    public void testDecompressors(String endpoint, String acceptEncoding, String contentEncoding, String method) {
        runDecompressorsTest(urlDEcompressed.toString() + endpoint, acceptEncoding, contentEncoding, method);
    }
}
