package io.quarkus.vertx.http.proxy;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.ForwardedHandlerInitializer;
import io.quarkus.vertx.http.proxy.fakedns.DnameRecordEncoder;
import io.quarkus.vertx.http.proxy.fakedns.DnsMessageEncoder;
import io.quarkus.vertx.http.proxy.fakedns.FakeDNSServer;
import io.restassured.RestAssured;

public class TrustedForwarderDnsResolveTest {

    private FakeDNSServer dnsServer;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ForwardedHandlerInitializer.class, DnameRecordEncoder.class, DnsMessageEncoder.class,
                            FakeDNSServer.class)
                    .addAsResource(new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n" +
                            "quarkus.http.proxy.allow-forwarded=true\n" +
                            "quarkus.http.proxy.enable-forwarded-host=true\n" +
                            "quarkus.http.proxy.enable-forwarded-prefix=true\n" +
                            "quarkus.vertx.resolver.servers=127.0.0.1:53530\n" +
                            "quarkus.http.proxy.trusted-proxies=trusted.example.com"),
                            "application.properties"));

    @BeforeEach
    public void setUp() throws Exception {
        dnsServer = new FakeDNSServer();
        dnsServer.start();
    }

    @AfterEach
    public void tearDown() {
        dnsServer.stop();
    }

    @Test
    public void testTrustedProxyResolved() {
        dnsServer.addRecordsToStore("trusted.example.com", "127.0.0.3", "127.0.0.2", "127.0.0.1");
        RestAssured.given()
                .header("Forwarded", "proto=http;for=backend2:5555;host=somehost2")
                .get("/path")
                .then()
                .body(Matchers.equalTo("http|somehost2|backend2:5555|/path|http://somehost2/path"));
    }

}
