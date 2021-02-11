package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ForwardedForHeaderTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClasses(ForwardedHandlerInitializer.class)
                            .addAsResource(
                                    new StringAsset("quarkus.http.proxy.proxy-address-forwarding=true\n"
                                            + "quarkus.http.proxy.enable-forwarded-host=true\n"),
                                    "application.properties"));

    @Test
    public void test() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given().header("X-Forwarded-Proto", "https").header("X-Forwarded-For", "backend:4444")
                .header("X-Forwarded-Host", "somehost").get("/forward").then()
                .body(Matchers.equalTo("https|somehost|backend:4444"));
    }

    @Test
    public void testIPV4WithPort() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given().header("X-Forwarded-Proto", "https").header("X-Forwarded-For", "192.168.42.123:4444")
                .header("X-Forwarded-Host", "somehost").get("/forward").then()
                .body(Matchers.equalTo("https|somehost|192.168.42.123:4444"));
    }

    @Test
    public void testIPV4NoPort() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given().header("X-Forwarded-Proto", "https").header("X-Forwarded-For", "192.168.42.123")
                .header("X-Forwarded-Host", "somehost").get("/forward").then()
                .body(Matchers.containsString("192.168.42.123"));
    }

    @Test
    public void testIPV6() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given().header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "2001:db8:85a3:8d3:1319:8a2e:370:12").header("X-Forwarded-Host", "somehost")
                .get("/forward").then().body(Matchers.containsString("2001:db8:85a3:8d3:1319:8a2e:370:12"));
    }

    @Test
    public void testIPV6HexEnding() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given().header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "2001:db8:85a3:8d3:1319:8a2e:370:ac").header("X-Forwarded-Host", "somehost")
                .get("/forward").then().body(Matchers.containsString("2001:db8:85a3:8d3:1319:8a2e:370:ac"));
    }

    @Test
    public void testIPV6Compressed() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given().header("X-Forwarded-Proto", "https").header("X-Forwarded-For", "2001:db8:85a3::12")
                .header("X-Forwarded-Host", "somehost").get("/forward").then()
                .body(Matchers.containsString("2001:db8:85a3::12"));
    }

    @Test
    public void testIPV6AnotherCompressed() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given().header("X-Forwarded-Proto", "https").header("X-Forwarded-For", "2001:db8:85a3::")
                .header("X-Forwarded-Host", "somehost").get("/forward").then()
                .body(Matchers.containsString("2001:db8:85a3::"));
    }

    @Test
    public void testIPV6WithPort() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given().header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-For", "[2001:db8:85a3:8d3:1319:8a2e:370:ac]:101")
                .header("X-Forwarded-Host", "somehost").get("/forward").then()
                .body(Matchers.containsString("[2001:db8:85a3:8d3:1319:8a2e:370:ac]:101"));
    }

    @Test
    public void testIPV6CompressedWithPort() {
        assertThat(RestAssured.get("/forward").asString()).startsWith("http|");

        RestAssured.given().header("X-Forwarded-Proto", "https").header("X-Forwarded-For", "[2001:db8:85a3:8d3::]:101")
                .header("X-Forwarded-Host", "somehost").get("/forward").then()
                .body(Matchers.containsString("[2001:db8:85a3:8d3::]:101"));
    }

}
