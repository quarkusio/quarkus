package io.quarkus.devui;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.util.IoUtil;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * With {@code quarkus.dev-ui.allow-loopback-hostnames=true} a host name that resolves to a loopback address is
 * accepted both as the request host and as the CORS origin; other host names are still rejected. The server's own
 * host validation has to let the host name through first, hence {@code require-localhost=false}.
 */
public class DevUILoopbackHostnameTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addAsResource(new StringAsset("""
                    quarkus.dev-ui.allow-loopback-hostnames=true
                    quarkus.http.host-validation.require-localhost=false
                    """), "application.properties"));

    @Test
    public void testPreflightFromLoopbackHostname() throws Exception {
        String hostname = LoopbackHostnames.find();
        String origin = "http://" + hostname + ":8080";
        String response = preflight(hostname + ":8080", origin);
        assertTrue(response.startsWith("HTTP/1.1 200"), response);
        assertTrue(response.contains("access-control-allow-origin: " + origin), response);
        assertTrue(response.contains("access-control-allow-methods: GET,POST"), response);
    }

    @Test
    public void testPreflightFromUnknownHostname() {
        RestAssured.given()
                .header("Origin", "http://devui.example.invalid")
                .header("Access-Control-Request-Method", "GET,POST")
                .when()
                .options("q/dev-ui/configuration-form-editor").then()
                .statusCode(403)
                .body(emptyOrNullString());
    }

    private static String preflight(String host, String origin) throws Exception {
        try (Socket socket = new Socket("localhost", 8080)) {
            socket.getOutputStream().write(("OPTIONS /q/dev-ui/configuration-form-editor HTTP/1.1\r\n"
                    + "Host: " + host + "\r\n"
                    + "Origin: " + origin + "\r\n"
                    + "Access-Control-Request-Method: GET,POST\r\n"
                    + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            return new String(IoUtil.readBytes(socket.getInputStream()), StandardCharsets.UTF_8);
        }
    }
}
