package io.quarkus.mailer.impl;

import static java.util.Arrays.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.mailer.Attachment;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.ReactiveMailer;
import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.file.AsyncFile;
import io.vertx.axle.ext.mail.MailClient;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailMessage;

@ApplicationScoped
public class ReactiveMailerImpl implements ReactiveMailer {

    private static final Logger LOGGER = LoggerFactory.getLogger("quarkus-mailer");

    @Inject
    MailClient client;

    @Inject
    Vertx vertx;

    /**
     * Default from value.
     */
    private String from;

    /**
     * Default bounce address.
     */
    private String bounceAddress;

    /**
     * If {@code true}, mails are not sent to the server, the body is printed in the console.
     */
    private boolean mock;

    @Override
    public CompletionStage<Void> send(Mail... mails) {
        if (mails == null) {
            throw new IllegalArgumentException("The `mails` parameter must not be `null`");
        }

        return allOf(
                stream(mails)
                        .map(mail -> toMailMessage(mail).thenCompose(this::send))
                        .collect(Collectors.toList()));
    }

    private CompletionStage<Void> send(MailMessage message) {
        if (mock) {
            LOGGER.info("Sending email {} from {} to {}, body is: \n{}",
                    message.getSubject(), message.getFrom(), message.getTo(),
                    message.getHtml() == null ? message.getText() : message.getHtml());
            return CompletableFuture.completedFuture(null);
        } else {
            return client.sendMail(message)
                    .thenAccept(x -> {
                    }); // Discard result.
        }
    }

    private CompletionStage<MailMessage> toMailMessage(Mail mail) {
        MailMessage message = new MailMessage();

        if (mail.getBounceAddress() != null) {
            message.setBounceAddress(mail.getBounceAddress());
        } else {
            message.setBounceAddress(this.bounceAddress);
        }

        if (mail.getFrom() != null) {
            message.setFrom(mail.getFrom());
        } else {
            message.setFrom(this.from);
        }

        message.setTo(mail.getTo());
        message.setCc(mail.getCc());
        message.setBcc(mail.getBcc());
        message.setSubject(mail.getSubject());
        message.setText(mail.getText());
        message.setHtml(mail.getHtml());
        message.setHeaders(toMultimap(mail.getHeaders()));

        List<CompletionStage<?>> stages = new ArrayList<>();
        List<MailAttachment> attachments = new CopyOnWriteArrayList<>();
        List<MailAttachment> inline = new CopyOnWriteArrayList<>();
        for (Attachment attachment : mail.getAttachments()) {
            if (attachment.isInlineAttachment()) {
                stages.add(toMailAttachment(attachment).thenAccept(inline::add));
            } else {
                stages.add(toMailAttachment(attachment).thenAccept(attachments::add));
            }
        }

        return allOf(stages).thenApply(x -> {
            message.setAttachment(attachments);
            message.setInlineAttachment(inline);
            return message;
        });
    }

    private MultiMap toMultimap(Map<String, List<String>> headers) {
        MultiMap mm = MultiMap.caseInsensitiveMultiMap();
        headers.forEach(mm::add);
        return mm;
    }

    private CompletionStage<MailAttachment> toMailAttachment(Attachment attachment) {
        MailAttachment attach = new MailAttachment();
        attach.setName(attachment.getName());
        attach.setContentId(attachment.getContentId());
        attach.setDescription(attachment.getDescription());
        attach.setDisposition(attachment.getDisposition());
        attach.setContentType(attachment.getContentType());

        if ((attachment.getFile() == null && attachment.getData() == null) // No content
                || (attachment.getFile() != null && attachment.getData() != null)) // Too much content
        {

            throw new IllegalArgumentException("An attachment must contain either a file or a raw data");
        }

        return getAttachmentStream(vertx, attachment).thenApply(attach::setData);
    }

    public static CompletionStage<Buffer> getAttachmentStream(Vertx vertx, Attachment attachment) {
        if (attachment.getFile() != null) {
            CompletionStage<AsyncFile> open = vertx.fileSystem().open(attachment.getFile().getAbsolutePath(),
                    new OpenOptions().setRead(true).setCreate(false));
            return ReactiveStreams
                    .fromCompletionStage(open)
                    .flatMap(af -> af.toPublisherBuilder().map(io.vertx.axle.core.buffer.Buffer::getDelegate)
                            .onTerminate(af::close))
                    .collect(Buffer::buffer, Buffer::appendBuffer)
                    .run();
        } else if (attachment.getData() != null) {
            Publisher<Byte> data = attachment.getData();
            return ReactiveStreams.fromPublisher(data)
                    .collect(Buffer::buffer, Buffer::appendByte)
                    .run();
        } else {
            CompletableFuture<Buffer> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Attachment has no data"));
            return future;
        }
    }

    private static CompletionStage<Void> allOf(List<CompletionStage<?>> stages) {
        CompletableFuture<?>[] array = stages.stream()
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(array);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void configure(Optional<String> from, Optional<String> bounceAddress, boolean mock) {
        this.from = from.orElse(null);
        this.bounceAddress = bounceAddress.orElse(null);
        this.mock = mock;
    }
}
