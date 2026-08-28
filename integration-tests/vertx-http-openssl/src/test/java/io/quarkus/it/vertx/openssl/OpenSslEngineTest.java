package io.quarkus.it.vertx.openssl;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.netty.handler.ssl.OpenSsl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * The application is configured with {@code quarkus.tls.ssl-engine=openssl} and
 * {@code quarkus.tls.pqc-enforcement-policy=client-negotiated}, so it can only start if the OpenSSL engine
 * (netty-tcnative + libssl 3.5+) is usable. The same test runs against the native executable in
 * {@link OpenSslEngineIT}.
 */
@QuarkusTest
@EnabledIf("isOpenSsl35Available")
public class OpenSslEngineTest {

    /**
     * Same condition as the JVM-mode PQC tests in extensions/vertx-http: the engine must be loadable in the test
     * JVM and the system libssl must be 3.5+ (ML-KEM hybrid groups). When it isn't, the application cannot start
     * and the test is skipped rather than failed.
     */
    static boolean isOpenSsl35Available() {
        try {
            return OpenSsl.isAvailable() && OpenSsl.version() >= 0x30500000L;
        } catch (Throwable t) {
            return false;
        }
    }

    @Test
    public void serversHttpsOverTheOpenSslEngine() {
        RestAssured.given().relaxedHTTPSValidation()
                .get("/ssl/hello")
                .then().statusCode(200).body(is("hello over TLS"));
    }

    @Test
    public void reportsTheOpenSslEngineNotTheJdkOne() {
        RestAssured.given().relaxedHTTPSValidation()
                .get("/ssl/engine")
                .then().statusCode(200)
                .body(containsString("OpenSsl"))
                .body(containsString("TLSv1.3"));
    }
}
