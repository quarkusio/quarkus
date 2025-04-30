package io.quarkus.websockets.next.test.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.smallrye.mutiny.Uni;

public class HttpUpgradeCheckHeaderMergingTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Headers.class, Header1HttpUpgradeCheck.class,
                            Header2HttpUpgradeCheck.class, Header3HttpUpgradeCheck.class, WSClient.class));

    @TestHTTPResource("headers")
    URI headersUri;

    @Test
    public void testHeadersMultiMap() {
        // this is a way to test scenario where HttpUpgradeChecks set headers
        // but the checks itself did not reject upgrade, the upgrade wasn't performed due to incorrect headers
        var headers = RestAssured.given()
                // without this header the client would receive 404
                .header("Sec-WebSocket-Key", "foo")
                .get(headersUri)
                .then()
                .statusCode(400)
                .extract()
                .headers();

        assertNotNull(headers);
        assertTrue(headers.size() >= 3);
        Stream.of("k", "k2", "k3").forEach(k -> {
            assertNotNull(headers.getList(k));
            var vals = headers.getList(k).stream().map(Header::getValue).toList();
            assertEquals(4, vals.size(), vals.toString());
            assertTrue(vals.contains("val1"), vals.toString());
            assertTrue(vals.contains("val2"), vals.toString());
            assertTrue(vals.contains("val3"), vals.toString());
            assertTrue(vals.contains("val4"), vals.toString());
        });
    }

    @Dependent
    public static class Header1HttpUpgradeCheck implements HttpUpgradeCheck {

        @Override
        public Uni<CheckResult> perform(HttpUpgradeContext context) {
            return CheckResult.permitUpgrade(Map.of("k", List.of("val1")));
        }
    }

    @Dependent
    public static class Header2HttpUpgradeCheck implements HttpUpgradeCheck {

        @Override
        public Uni<CheckResult> perform(HttpUpgradeContext context) {
            return CheckResult.permitUpgrade(Map.of("k", List.of("val2", "val3", "val4"), "k2", List.of("val1")));
        }
    }

    @Dependent
    public static class Header3HttpUpgradeCheck implements HttpUpgradeCheck {

        @Override
        public Uni<CheckResult> perform(HttpUpgradeContext context) {
            return CheckResult.permitUpgrade(
                    Map.of("k3", List.of("val1", "val2", "val3", "val4"), "k2", List.of("val2", "val3", "val4")));
        }
    }

    @WebSocket(path = "/headers")
    public static class Headers {

        @OnTextMessage
        public String onMessage(String message) {
            return "Hola " + message;
        }

    }
}
