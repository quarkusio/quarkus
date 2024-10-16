package io.quarkus.it.mailer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.it.mailer.mailpit.MailPitClient;
import io.quarkus.it.mailer.mailpit.Message;
import io.quarkus.it.mailer.mailpit.Recipient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;

/**
 * Base Mailer tests using a plain TCP connection to the SMTP server.
 */
@DisabledOnOs({ OS.WINDOWS })
@QuarkusTest
@QuarkusTestResource(MailpitTestResource.class)
@QuarkusTestResource(MailpitFullTlsTestResource.class)
public class MailerTest {
    private MailPitClient client;
    private MailPitClient clientTls;

    @BeforeEach
    public void init() {
        client = new MailPitClient("http://" + ConfigProvider.getConfig().getValue("mailpit", String.class));
        clientTls = new MailPitClient("http://" + ConfigProvider.getConfig().getValue("mailpit-tls", String.class));
    }

    @AfterEach
    public void clearAllReceivedEmails() {
        client.deleteAllMessages();
        clientTls.deleteAllMessages();
    }

    @Test
    public void sendTextEmail() {
        RestAssured.get("/mail/text");

        Message email = getLastEmail();

        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("simple test email");

        assertThat(email.to().address()).contains("nobody@quarkus.io");

        String content = client.getMessageTextContent(email);
        assertThat(content).contains("This is a simple test email.");
    }

    @Test
    public void sendTextEmailWithNonAsciiCharacters() {
        RestAssured.get("/mail/text-non-ascii");

        Message email = getLastEmail();
        assertThat(email).isNotNull();
        assertThat(email.subject).contains("Příliš žluťoučký kůň úpěl ďábelské ódy na 各分野最高のライブラリと標準で構成された、");

        String content = client.getMessageTextContent(email);
        assertThat(content).contains("Příliš žluťoučký kůň úpěl ďábelské ódy na 各分野最高のライブラリと標準で構成された、");
    }

    @Test
    public void sendEmailWithHumanFriendlyAddressPrefix() {
        RestAssured.get("/mail/human-friendly-address");

        Message email = getLastEmail();
        assertThat(email).isNotNull();
        assertThat(email.from.name()).contains("Roger the robot");
        assertThat(email.to().name()).contains("Mr. Nobody");
    }

    @Test
    public void checkAttachmentCache() {
        String body = RestAssured.get("/mail/attachments/cache").then().extract().asString();
        assertThat(body).isEqualTo("true");
    }

    @Test
    public void sendTextEmailWithDkimSignature() {
        RestAssured.get("/mail/dkim");

        Message email = getLastEmail();
        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("simple test email");
        assertThat(email.to().address()).contains("nobody@quarkus.io");

        String content = client.getMessageTextContent(email);
        assertThat(content).contains("This is a simple test email.");

        JsonObject headers = client.getMessageHeaders(email);
        assertThat(headers.getJsonArray("Dkim-Signature")).isNotNull().isNotEmpty();
        assertThat(headers.getJsonArray("Dkim-Signature").getString(0)).isNotNull().isNotBlank();
        String signature = headers.getJsonArray("Dkim-Signature").getString(0);
        assertThat(signature).contains(
                "v=1; a=rsa-sha256; c=simple/relaxed; d=quarkus.io; i=roger-the-robot@quarkus.io; s=exampleUser; h=From:To; l=5000; bh=A6TE38YlUzdJNIgFMcUU43Wde0+0vzij4DoME0Gtnqk=; b=FWVms0SnIyX9F5pgdUHgIy+aJnjBNrcToJzBdBiKb6fYZ7cbrUhqmrUkYI2MsIXk4D2DvCig+eU8eqES65Slwc6pMo6ZWKEmqDQcr/2fAx0x30p/tbCemivcRmInaeoxit0cSsoAnnvv+XRuWc9rAAajuI1DCo+Pw7pZBUkKz0M=");
    }

    @Test
    public void sendEmailToMultipleRecipients() {
        RestAssured.get("/mail/multiple-recipients");

        Message email = getLastEmail();

        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("simple test email");
        assertThat(email.to.stream().map(Recipient::address).collect(Collectors.toList())).contains("nobody@quarkus.io",
                "nobody@example.com");

        String content = client.getMessageTextContent(email);
        assertThat(content).contains("This is a simple test email.");
    }

    @Test
    public void sendHtmlEmail() {
        RestAssured.get("/mail/html");

        Message email = getLastEmail();

        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("html test email");

        assertThat(email.to().address()).contains("nobody@quarkus.io");

        String content = client.getMessageHtmlContent(email);
        assertThat(content).contains("<h3>Hello!</h3>");
    }

    @Test
    public void sendSimpleHtmlEmailWithAttachmentInline() {
        RestAssured.get("/mail/html-inline-attachment");

        await().until(() -> getLastEmail() != null);

        Message email = getLastEmail();

        assertThat(email).isNotNull();

        String content = client.getMessageHtmlContent(email);
        assertThat(content).contains("<img src=");

        String raw = client.getRawSource(email);
        assertThat(raw).contains("<img src=\"cid:my-file@quarkus.io\"/>");
        assertThat(raw).contains("Content-ID: <my-file@quarkus.io>");
    }

    @Test
    public void sendTextEmailWithAttachment() {
        RestAssured.get("/mail/text-with-attachment");

        Message email = getLastEmail();
        assertThat(email).isNotNull();

        assertThat(email.subject).isEqualTo("simple test email with an attachment");
        assertThat(client.getMessageTextContent(email)).contains("Roger the robot");

        assertThat(client.getAttachments(email)).hasSize(1)
                .allSatisfy(attachment -> {
                    assertThat(attachment.filename).isEqualTo("lorem.txt");
                    assertThat(attachment.contentType).isEqualTo("text/plain");
                    assertThat(new String(attachment.binary)).contains("Lorem ipsum dolor sit amet");
                });
    }

    @Test
    public void sendEmailFromTemplate() {
        RestAssured.get("/mail/text-from-template");

        Message email = getLastEmail();
        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("template mail");
        assertThat(client.getMessageTextContent(email)).contains("Hello John!");
        assertThat(email.to().address()).isEqualTo("nobody@quarkus.io");
    }

    @Test
    public void sendEmailWithHeaders() {
        RestAssured.get("mail/text-with-headers");

        Message email = getLastEmail();
        assertThat(email).isNotNull();

        assertThat(email.to().address()).isEqualTo("nobody@quarkus.io");
        assertThat(email.subject).isEqualTo("simple test email");

        JsonObject headers = client.getMessageHeaders(email);
        assertThat(headers).isNotNull().isNotEmpty();
        assertThat(headers.getJsonArray("Accept").getString(0)).isEqualTo("http");
    }

    @ParameterizedTest
    @ValueSource(strings = { "start-tls-legacy", "start-tls-legacy-trust-all", "start-tls-registry",
            "start-tls-registry-trust-all" })
    public void sendTextEmailUsingStartTls(String mailerName) {
        RestAssured.get("/mail/text/" + mailerName);

        Message email = getLastEmail();

        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("simple test email " + mailerName);

        assertThat(email.to().address()).contains("nobody@quarkus.io");

        String content = client.getMessageTextContent(email);
        assertThat(content).contains("This is a simple test email.");
    }

    @ParameterizedTest
    @ValueSource(strings = { "tls-legacy", "tls-legacy-trust-all", "tls-registry", "tls-registry-trust-all" })
    public void sendTextEmailUsingTls(String mailerName) {
        RestAssured.get("/mail/text/" + mailerName);

        Message email = getLastEmail(clientTls);

        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("simple test email " + mailerName);

        assertThat(email.to().address()).contains("nobody@quarkus.io");

        String content = clientTls.getMessageTextContent(email);
        assertThat(content).contains("This is a simple test email.");
    }

    private Message getLastEmail() {
        return getLastEmail(client);
    }

    private void awaitForEmail(MailPitClient client) {
        await().until(() -> client.getMessages() != null && !client.getMessages().messages.isEmpty());
    }

    private Message getLastEmail(MailPitClient client) {
        awaitForEmail(client);
        return client.getMessages().messages.get(client.getMessages().messages.size() - 1);
    }

}
