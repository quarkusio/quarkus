package io.quarkus.smallrye.graphql.deployment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;

/**
 * Regression test for <a href="https://github.com/quarkusio/quarkus/issues/29141">#29141</a>
 * and <a href="https://github.com/quarkusio/quarkus/issues/36215">#36215</a>.
 * <p>
 * Verifies that a blocking GraphQL resolver can call the imperative {@link Mailer}
 * without deadlocking. The Mailer internally uses the reactive Vert.x SMTP client,
 * and the blocking wrapper calls {@code .await().indefinitely()}. With ordered
 * {@code executeBlocking}, this deadlocked because the SMTP client's connection
 * establishment callbacks couldn't dispatch while the ordered task slot was held.
 * <p>
 * Uses a minimal fake SMTP server to exercise the real Vert.x MailClient code path
 * (not the mock mailer, which bypasses the SMTP client entirely).
 */
public class BlockingResolverReactiveCallbackTest extends AbstractGraphQLTest {

    private static final Logger LOG = Logger.getLogger(BlockingResolverReactiveCallbackTest.class);

    static final ServerSocket smtpServer;
    static final int smtpPort;

    static {
        try {
            smtpServer = new ServerSocket(0);
            smtpPort = smtpServer.getLocalPort();
            Thread smtpThread = new Thread(() -> {
                while (!smtpServer.isClosed()) {
                    try {
                        Socket client = smtpServer.accept();
                        new Thread(() -> handleSmtpSession(client)).start();
                    } catch (Exception e) {
                        if (!smtpServer.isClosed()) {
                            LOG.error("Error accepting SMTP connection", e);
                        }
                    }
                }
            }, "fake-smtp-acceptor");
            smtpThread.setDaemon(true);
            smtpThread.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start fake SMTP server", e);
        }
    }

    @AfterAll
    static void stopFakeSmtp() throws Exception {
        if (smtpServer != null) {
            smtpServer.close();
        }
    }

    static void handleSmtpSession(Socket client) {
        try (client;
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            out.println("220 localhost SMTP ready");
            String line;
            while ((line = in.readLine()) != null) {
                String cmd = line.toUpperCase();
                if (cmd.startsWith("EHLO") || cmd.startsWith("HELO")) {
                    out.println("250 OK");
                } else if (cmd.startsWith("MAIL FROM")) {
                    out.println("250 OK");
                } else if (cmd.startsWith("RCPT TO")) {
                    out.println("250 OK");
                } else if (cmd.startsWith("DATA")) {
                    out.println("354 Start mail input");
                    while ((line = in.readLine()) != null) {
                        if (".".equals(line)) {
                            break;
                        }
                    }
                    out.println("250 OK");
                } else if (cmd.startsWith("QUIT")) {
                    out.println("221 Bye");
                    break;
                } else if (cmd.startsWith("RSET")) {
                    out.println("250 OK");
                } else {
                    out.println("500 Unknown command");
                }
            }
        } catch (Exception e) {
            LOG.debug("SMTP client disconnected", e);
        }
    }

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(MailerResource.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.mailer.mock", "false")
            .overrideConfigKey("quarkus.mailer.host", "localhost")
            .overrideConfigKey("quarkus.mailer.port", String.valueOf(smtpPort))
            .overrideConfigKey("quarkus.mailer.from", "test@example.com");

    @Test
    public void blockingResolverCanSendMail() {
        String request = getPayload("{ sendMail }");
        RestAssured.given()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .statusCode(200)
                .body("data.sendMail", CoreMatchers.equalTo("sent"));
    }

    @GraphQLApi
    public static class MailerResource {

        @Inject
        Mailer mailer;

        @Query
        @Blocking
        public String sendMail() {
            mailer.send(Mail.withText("to@example.com", "Test", "Hello from GraphQL"));
            return "sent";
        }
    }
}
