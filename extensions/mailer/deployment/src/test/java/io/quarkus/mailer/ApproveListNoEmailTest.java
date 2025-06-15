package io.quarkus.mailer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ApproveListNoEmailTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addAsResource(new StringAsset(
                    "quarkus.mailer.approved-recipients=.*@approved1.com\nquarkus.mailer.log-rejected-recipients=true"),
                    "application.properties"))
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.WARNING)).assertLogRecords(lrs -> {
                assertTrue(lrs.stream().anyMatch(lr -> lr.getMessage().equals(
                        "Email 'A subject' was not sent because all recipients were rejected by the configuration: [email1@rejected.com, email2@rejected.com, email3@rejected.com, email4@rejected.com, email5@rejected.com]")));
            });

    @Inject
    Mailer mailer;

    @Inject
    MockMailbox mockMailbox;

    @Test
    public void testApproveList() {
        mailer.send(Mail.withText("email1@rejected.com", "A subject", "")
                .addCc("email2@rejected.com", "email3@rejected.com")
                .addBcc("email4@rejected.com", "email5@rejected.com"));

        assertEquals(0, mockMailbox.getMailMessagesSentTo("email1@rejected.com").size());
        assertEquals(0, mockMailbox.getMailMessagesSentTo("email2@rejected.com").size());
        assertEquals(0, mockMailbox.getMailMessagesSentTo("email3@rejected.com").size());
        assertEquals(0, mockMailbox.getMailMessagesSentTo("email4@rejected.com").size());
        assertEquals(0, mockMailbox.getMailMessagesSentTo("email5@rejected.com").size());
    }
}
