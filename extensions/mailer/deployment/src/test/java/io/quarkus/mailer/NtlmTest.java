package io.quarkus.mailer;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Verify NTLM authentication.
 */
public class NtlmTest extends FakeSmtpTestBase {

    @SuppressWarnings("unused")
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FakeSmtpServer.class)
                    .addAsResource("ntlm-config.properties", "application.properties"));

    @BeforeEach
    public void init() {
        startServer(null);
    }

    @Inject
    Mailer mailer;

    @Test
    public void testNTLM() {
        super.smtpServer.setDialogue(
                "220 HK2P15301CA0023.outlook.office365.com Microsoft ESMTP MAIL Service",
                "EHLO localhost",
                "250-HK2P15301CA0023.outlook.office365.com Hello [209.132.188.80]\n" +
                        "250-SIZE 157286400\n" +
                        "250-PIPELINING\n" +
                        "250-AUTH NTLM\n" +
                        "250-ENHANCEDSTATUSCODES\n" +
                        "250-CHUNKING\n" +
                        "250 SMTPUTF8",
                "AUTH NTLM TlRMTVNTUAABAAAAAYIIogAAAAAoAAAAAAAAACgAAAAFASgKAAAADw==",
                "334 TlRMTVNTUAACAAAAFgAWADgAAAA1goriZt7rI6Uq/ccAAAAAAAAAAGwAbABOAAAABQLODgAAAA9FAFgAQwBIAC0AQwBMAEkALQA2ADYAAgAWAEUAWABDAEgALQBDAEwASQAtADYANgABABYARQBYAEMASAAtAEMATABJAC0ANgA2AAQAFgBlAHgAYwBoAC0AYwBsAGkALQA2ADYAAwAWAGUAeABjAGgALQBjAGwAaQAtADYANgAAAAAA",
                "^TlRMTVNT[^\n]*",
                "235 2.7.0 Authentication successful",
                "MAIL FROM",
                "250 2.1.0 Ok",
                "RCPT TO",
                "250 2.1.5 Ok",
                "DATA",
                "354 End data with <CR><LF>.<CR><LF>",
                "250 2.0.0 Ok: queued as ABCD",
                "QUIT",
                "221 2.0.0 Bye");
        mailer.send(getMail());
        stopVertx();
    }
}
