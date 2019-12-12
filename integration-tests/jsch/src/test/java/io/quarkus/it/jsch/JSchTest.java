package io.quarkus.it.jsch;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.core.Is.is;

import java.net.InetAddress;
import java.nio.file.Files;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.hostbased.AcceptAllHostBasedAuthenticator;
import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.UnknownCommandFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JSchTest {

    private SshServer sshd;

    @BeforeEach
    public void setupSSHDServer() throws Exception {
        sshd = SshServer.setUpDefaultServer();
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Files.createTempFile("host", "key")));
        sshd.setHostBasedAuthenticator(AcceptAllHostBasedAuthenticator.INSTANCE);
        sshd.setPasswordAuthenticator(AcceptAllPasswordAuthenticator.INSTANCE);
        sshd.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
        sshd.setCommandFactory(UnknownCommandFactory.INSTANCE);
        sshd.setHost(InetAddress.getLocalHost().getHostName());
        sshd.start();
    }

    @Test
    void shouldConnect() {
        given().queryParam("host", sshd.getHost())
                .queryParam("port", sshd.getPort())
                .get("/jsch")
                .then()
                .statusCode(is(200))
                .body(endsWith(sshd.getVersion()));
    }

    @AfterEach
    void stopServer() throws Exception {
        if (sshd != null) {
            sshd.stop(true);
        }
    }
}
