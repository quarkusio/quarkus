package io.quarkus.virtual.mail;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MailTemplate;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;

@Path("/")
@RunOnVirtualThread
public class MailerResource {

    @Inject
    Mailer mailer;

    @GET
    public String send() {
        VirtualThreadsAssertions.assertEverything();
        mailer.send(Mail.withText("roger-the-robot@quarkus.io", "test simple", "This email is sent from a virtual thread"));
        return "OK";
    }

    @GET
    @Path("/template")
    public String sendWithTemplate() {
        VirtualThreadsAssertions.assertEverything();
        Templates.hello("virtual threads").to("roger-the-robot@quarkus.io").subject("test template").send().await()
                .atMost(Duration.ofSeconds(3));
        return "OK";
    }

    @CheckedTemplate
    static class Templates {
        public static native MailTemplate.MailTemplateInstance hello(String name);
    }

}
