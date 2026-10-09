package io.quarkus.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.util.IoUtil;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIConfiguredHostTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addAsResource(new StringAsset("""
                    quarkus.http.host-validation.allowed-hosts=localhost,my-machine,other-machine
                    quarkus.dev-ui.hosts=my-machine
                    """), "application.properties"));

    @Test
    public void configuredHostIsAccepted() throws Exception {
        String response = preflight("my-machine");
        assertThat(response).startsWith("HTTP/1.1 200");
        assertThat(response).contains("access-control-allow-origin: http://my-machine:8080");
    }

    @Test
    public void hostThatIsNotConfiguredForDevUIIsRejected() throws Exception {
        assertThat(preflight("other-machine")).startsWith("HTTP/1.1 403");
    }

    @Test
    public void originThatOnlyStartsWithConfiguredHostIsRejected() throws Exception {
        assertThat(preflight("my-machine", "http://my-machine.attacker.example")).startsWith("HTTP/1.1 403");
    }

    private static String preflight(String host) throws Exception {
        return preflight(host, "http://" + host + ":8080");
    }

    private static String preflight(String host, String origin) throws Exception {
        try (Socket socket = new Socket("localhost", 8080)) {
            socket.getOutputStream().write(("OPTIONS /q/dev-ui/configuration-form-editor HTTP/1.1\r\n"
                    + "Host: " + host + ":8080\r\n"
                    + "Origin: " + origin + "\r\n"
                    + "Access-Control-Request-Method: GET,POST\r\n"
                    + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            return new String(IoUtil.readBytes(socket.getInputStream()), StandardCharsets.UTF_8);
        }
    }
}
