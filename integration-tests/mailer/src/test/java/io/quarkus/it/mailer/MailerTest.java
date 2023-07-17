package io.quarkus.it.mailer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.lang.reflect.Type;
import java.util.List;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.google.common.reflect.TypeToken;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@DisabledOnOs({ OS.WINDOWS, OS.MAC })
@QuarkusTest
@QuarkusTestResource(FakeMailerTestResource.class)
public class MailerTest {
    public static final String LOREM_CHECKSUM = "386e8d6ae186eacb5b60fac15ed140cd";
    private String mailServer;

    @BeforeEach
    public void init() {
        mailServer = "http://" + ConfigProvider.getConfig().getValue("fake.mailer", String.class) + "/api/emails";
    }

    @AfterEach
    public void clearAllReceivedEmails() {
        RestAssured.delete(mailServer)
                .then().statusCode(200);
    }

    @Test
    public void sendTextEmail() {
        RestAssured.get("/mail/text");

        await().until(() -> getLastEmail() != null);

        TextEmail email = getLastEmail();
        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("simple test email");
        assertThat(email.text).contains("This is a simple test email.");
        assertThat(email.to.text).contains("nobody@quarkus.io");
    }

    @Test
    public void sendTextEmailWithNonAsciiCharacters() {
        RestAssured.get("/mail/text-non-ascii");

        await().until(() -> getLastEmail() != null);

        TextEmail email = getLastEmail();
        assertThat(email).isNotNull();
        assertThat(email.subject).contains("Příliš žluťoučký kůň úpěl ďábelské ódy na 各分野最高のライブラリと標準で構成された、");
        assertThat(email.text).contains("Příliš žluťoučký kůň úpěl ďábelské ódy na 各分野最高のライブラリと標準で構成された、");
    }

    @Test
    public void sendEmailWithHumanFriendlyAddressPrefix() {
        RestAssured.get("/mail/human-friendly-address");

        await().until(() -> getLastEmail() != null);

        TextEmail email = getLastEmail();
        assertThat(email).isNotNull();
        assertThat(email.from.text).contains("Roger the robot <roger-the-robot@quarkus.io>");
        assertThat(email.to.text).contains("Mr. Nobody <nobody@quarkus.io>");
    }

    @Test
    public void checkAttachmentCache() {
        String body = RestAssured.get("/mail/attachments/cache").then().extract().asString();
        assertThat(body).isEqualTo("true");
    }

    @Test
    public void sendTextEmailWithDkimSignature() {
        RestAssured.get("/mail/dkim");

        await().until(() -> getLastEmail() != null);

        TextEmail email = getLastEmail();
        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("simple test email");
        assertThat(email.text).contains("This is a simple test email.");
        assertThat(email.to.text).contains("nobody@quarkus.io");

        String signature = email.headerLines.stream().filter(h -> h.key.equalsIgnoreCase("DKIM-Signature")).findAny()
                .get().line;
        assertThat(signature).contains(
                "DKIM-Signature: v=1; a=rsa-sha256; c=simple/relaxed; d=quarkus.io; i=roger-the-robot@quarkus.io; s=exampleUser; h=From:To; l=5000; bh=A6TE38YlUzdJNIgFMcUU43Wde0+0vzij4DoME0Gtnqk=; b=FWVms0SnIyX9F5pgdUHgIy+aJnjBNrcToJzBdBiKb6fYZ7cbrUhqmrUkYI2MsIXk4D2DvCig+eU8eqES65Slwc6pMo6ZWKEmqDQcr/2fAx0x30p/tbCemivcRmInaeoxit0cSsoAnnvv+XRuWc9rAAajuI1DCo+Pw7pZBUkKz0M=");
    }

    @Test
    public void sendEmailToMultipleRecipients() {
        RestAssured.get("/mail/multiple-recipients");

        await().until(() -> getLastEmail() != null);

        TextEmail email = getLastEmail();
        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("simple test email");
        assertThat(email.text).contains("This is a simple test email.");
        assertThat(email.to.text).contains("nobody@quarkus.io", "nobody@example.com");
    }

    @Test
    public void sendHtmlEmail() {
        RestAssured.get("/mail/html");

        await().until(() -> getLastEmail() != null);

        HtmlEmail email = getLastHtmlEmail();
        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("html test email");
        assertThat(email.html).contains("<h3>Hello!</h3>");
        assertThat(email.to.text).contains("nobody@quarkus.io");
    }

    @Test
    public void sendSimpleHtmlEmailWithAttachmentInline() {
        RestAssured.get("/mail/html-inline-attachment");

        await().until(() -> getLastEmail() != null);

        HtmlEmail email = getLastHtmlEmail();
        assertThat(email).isNotNull();
        assertThat(email.html).contains("<img src=\"data:image/png;base64,");
    }

    @Test
    public void sendTextEmailWithAttachment() {
        RestAssured.get("/mail/text-with-attachment");

        await().until(() -> getLastEmail() != null);

        TextEmail email = getLastEmail();
        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("simple test email with an attachment");
        assertThat(email.text).contains("This is a simple test email.");
        assertThat(email.to.text).contains("nobody@quarkus.io");

        assertThat(email.attachments).hasSize(1)
                .allSatisfy(attachment -> {
                    assertThat(attachment.filename).isEqualTo("lorem.txt");
                    assertThat(attachment.checksum).isEqualTo(LOREM_CHECKSUM);
                    assertThat(attachment.contentDisposition).isEqualTo("attachment");
                    assertThat(attachment.contentType).isEqualTo("text/plain");
                });
    }

    @Test
    public void sendEmailFromTemplate() {
        RestAssured.get("/mail/text-from-template");

        await().until(() -> getLastEmail() != null);

        TextEmail email = getLastEmail();
        assertThat(email).isNotNull();
        assertThat(email.subject).isEqualTo("template mail");
        assertThat(email.text).contains("Hello John!");
        assertThat(email.to.text).isEqualTo("nobody@quarkus.io");
    }

    @Test
    public void sendEmailWithHeaders() {
        RestAssured.get("mail/text-with-headers");

        await().until(() -> getLastEmail() != null);

        TextEmail email = getLastEmail();
        assertThat(email).isNotNull();
        assertThat(email.to.text).isEqualTo("nobody@quarkus.io");
        assertThat(email.subject).isEqualTo("simple test email");
        assertThat(email.headerLines).isNotNull();
        assertThat(email.headerLines).anySatisfy(header -> assertThat(header.line).isEqualTo("Accept: http"));
    }

    @SuppressWarnings("UnstableApiUsage")
    private TextEmail getLastEmail() {
        Type t = new TypeToken<List<TextEmail>>() {
        }.getType();
        List<TextEmail> emails = RestAssured.get(mailServer).as(t);
        if (emails == null || emails.isEmpty()) {
            return null;
        }
        return emails.get(emails.size() - 1);
    }

    @SuppressWarnings("UnstableApiUsage")
    private HtmlEmail getLastHtmlEmail() {
        Type t = new TypeToken<List<HtmlEmail>>() {
        }.getType();
        List<HtmlEmail> emails = RestAssured.get(mailServer).as(t);
        if (emails == null || emails.isEmpty()) {
            return null;
        }
        return emails.get(emails.size() - 1);
    }

}
