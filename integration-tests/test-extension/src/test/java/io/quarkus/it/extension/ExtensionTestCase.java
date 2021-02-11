package io.quarkus.it.extension;

import static org.hamcrest.Matchers.is;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ExtensionTestCase {
    /**
     * Test the RuntimeXmlConfigService using old school sockets
     */
    @Test
    public void testRuntimeXmlConfigService() throws Exception {
        // From config.xml
        Socket socket = new Socket("localhost", 12345);
        OutputStream os = socket.getOutputStream();
        os.write("testRuntimeXmlConfigService\n".getBytes("UTF-8"));
        os.flush();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String reply = reader.readLine();
            Assertions.assertEquals("testRuntimeXmlConfigService-ack", reply);
        }
        socket.close();
    }

    @Test
    public void verifyCommandServlet() {
        RestAssured.when().get("/commands/ping").then()
                .body(is("/ping-ack"));
    }

}
