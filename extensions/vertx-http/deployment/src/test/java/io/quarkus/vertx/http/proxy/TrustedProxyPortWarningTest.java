package io.quarkus.vertx.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.restassured.RestAssured;

/**
 * A port in a trusted proxy address is compared with the source port of the connection from the proxy, which is
 * normally ephemeral, so such an entry is warned about and rarely matches.
 */
public class TrustedProxyPortWarningTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ForwardedHandlerInitializer.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n" +
                            "quarkus.http.proxy.allow-forwarded=true\n" +
                            "quarkus.http.proxy.enable-forwarded-host=true\n" +
                            "quarkus.http.proxy.enable-forwarded-prefix=true\n" +
                            "quarkus.http.proxy.trusted-proxies=127.0.0.1:8084"),
                            "application.properties"))
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.WARNING)
                    && record.getMessage().contains("trusted-proxies"))
            .assertLogRecords(records -> assertThat(records)
                    .map(record -> String.format(record.getMessage(), record.getParameters()))
                    .anyMatch(message -> message.contains("127.0.0.1:8084") && message.contains("source port")));

    @Test
    public void testHeadersAreIgnored() {
        RestAssured.given()
                .header("Forwarded", "by=proxy;for=\"[2001:db8:cafe::17]:47011\",for=backend:4444;host=somehost;proto=https")
                .get("/forward")
                .then()
                .body(Matchers.startsWith("http|localhost:8081|127.0.0.1:"));
    }
}
