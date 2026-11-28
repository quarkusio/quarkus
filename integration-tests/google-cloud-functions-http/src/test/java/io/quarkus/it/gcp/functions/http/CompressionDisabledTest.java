package io.quarkus.it.gcp.functions.http;

import static io.quarkus.it.gcp.functions.http.FunctionInvoker.body;
import static io.quarkus.it.gcp.functions.http.FunctionInvoker.staticFileBody;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

@QuarkusTest
@TestProfile(CompressionDisabledTest.Profile.class)
public class CompressionDisabledTest {

    private static FunctionInvoker function;

    public static class Profile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.virtual", "true",
                    "quarkus.http.host-enabled", "false");
        }
    }

    @BeforeAll
    static void startFunction() throws Exception {
        function = new FunctionInvoker();
    }

    @AfterAll
    static void stopFunction() throws Exception {
        function.close();
    }

    @Test
    public void testCompressionDisabled() throws IOException {
        Response response = function.get("/compression/compressed", "gzip");
        assertThat(response.header("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionRoutes.MESSAGE);

        response = function.get("/compression.txt", "gzip");
        assertThat(response.header("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(staticFileBody());

        response = function.get("/compression/chunked", "gzip");
        assertThat(response.header("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionRoutes.body());
    }
}
