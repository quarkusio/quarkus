package io.quarkus.it.amazon.ses;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest.Builder;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

@Path("/ses")
public class SesResource {

    private static final Logger LOG = Logger.getLogger(SesResource.class);
    public final static String EMAIL_TXT_BODY = "Quarkus is awsome";
    public final static String EMAIL_SUBJECT = "Quarkus";
    public final static String SYNC_FROM = "sync-sender@example.com";
    public final static String SYNC_TO = "sync-recipient@example.com";
    public final static String ASYNC_FROM = "async-sender@example.com";
    public final static String ASYNC_TO = "async-recipient@example.com";

    @Inject
    SesClient sesClient;

    @Inject
    SesAsyncClient sesAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync SES client");
        //Verify email recipients
        sesClient.verifyEmailIdentity(email -> email.emailAddress(SYNC_TO));
        sesClient.verifyEmailIdentity(email -> email.emailAddress(SYNC_FROM));
        //Send email
        SendEmailResponse sendEmailResponse = null;
        try {
            sendEmailResponse = sesClient.sendEmail(emailRequest());
        } catch (Exception ex) {
            LOG.error("Error sending email", ex);
        }
        return sendEmailResponse.messageId();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async SES client");

        return sesAsyncClient.verifyEmailIdentity(email -> email.emailAddress(ASYNC_TO))
                .thenCompose(r -> sesAsyncClient.verifyEmailIdentity(email -> email.emailAddress(ASYNC_FROM)))
                .thenCompose(r -> sesAsyncClient.sendEmail(emailRequest()))
                .thenApply(SendEmailResponse::messageId);
    }

    private Consumer<Builder> emailRequest() {
        return req -> req
                .destination(dest -> dest.toAddresses(SYNC_TO))
                .message(msg -> msg
                        .body(body -> body.text(text -> text.data(EMAIL_TXT_BODY)))
                        .subject(s -> s.data(EMAIL_SUBJECT)))
                .source(SYNC_FROM);
    }
}
