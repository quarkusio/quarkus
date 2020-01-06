package io.quarkus.mailer.runtime;

import static java.util.Arrays.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.reactivestreams.Publisher;

import io.quarkus.mailer.Attachment;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.mutiny.ReactiveMailer;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailMessage;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.file.AsyncFile;
import io.vertx.mutiny.ext.mail.MailClient;

@ApplicationScoped
public class MutinyMailerImpl implements ReactiveMailer {

    private static final Logger LOGGER = Logger.getLogger("quarkus-mailer");

    @Inject
    MailClient client;

    @Inject
    Vertx vertx;

    @Inject
    MockMailboxImpl mockMailbox;

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
    public Uni<Void> send(Mail... mails) {
        if (mails == null) {
            throw new IllegalArgumentException("The `mails` parameter must not be `null`");
        }

        List<Uni<Void>> unis = stream(mails)
                .map(mail -> toMailMessage(mail)
                        .onItem().produceUni(mailMessage -> send(mail, mailMessage)))
                .collect(Collectors.toList());

        return Uni.combine().all().unis(unis).combinedWith(results -> null);
    }

    private Uni<Void> send(Mail mail, MailMessage message) {
        if (mock) {
            LOGGER.infof("Sending email %s from %s to %s, text body: \n%s\nhtml body: \n%s",
                    message.getSubject(), message.getFrom(), message.getTo(),
                    message.getText(), message.getHtml());
            return mockMailbox.send(mail);
        } else {
            return client.sendMail(message)
                    .onItem().ignore().andContinueWithNull();
        }
    }

    private Uni<MailMessage> toMailMessage(Mail mail) {
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
        if (mail.getReplyTo() != null) {
            message.addHeader("Reply-To", mail.getReplyTo());
        }

        List<Uni<MailAttachment>> stages = new ArrayList<>();
        List<MailAttachment> attachments = new CopyOnWriteArrayList<>();
        List<MailAttachment> inline = new CopyOnWriteArrayList<>();
        for (Attachment attachment : mail.getAttachments()) {
            if (attachment.isInlineAttachment()) {
                stages.add(toMailAttachment(attachment)
                        .onItem().invoke(inline::add));
            } else {
                stages.add(toMailAttachment(attachment)
                        .onItem().invoke(attachments::add));
            }
        }

        if (stages.isEmpty()) {
            message.setAttachment(attachments);
            message.setInlineAttachment(inline);
            return Uni.createFrom().item(message);
        }

        Uni<List<MailAttachment>> uni = Uni.combine().all().unis(stages).combinedWith(Function.identity());
        return uni
                .onItem().apply(ignored -> {
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

    private Uni<MailAttachment> toMailAttachment(Attachment attachment) {
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

        return getAttachmentStream(vertx, attachment)
                .onItem().apply(attach::setData);
    }

    public static Uni<Buffer> getAttachmentStream(Vertx vertx, Attachment attachment) {
        if (attachment.getFile() != null) {
            Uni<AsyncFile> open = vertx.fileSystem().open(attachment.getFile().getAbsolutePath(),
                    new OpenOptions().setRead(true).setCreate(false));
            return open
                    .onItem().produceMulti(af -> af.toMulti()
                            .onItem().apply(io.vertx.mutiny.core.buffer.Buffer::getDelegate)
                            .on().termination((failure, cancellation) -> af.closeAndForget()))
                    .collectItems().with(Collector.of(Buffer::buffer, Buffer::appendBuffer, (b1, b2) -> b1));
        } else if (attachment.getData() != null) {
            Publisher<Byte> data = attachment.getData();
            return Multi.createFrom().publisher(data)
                    .collectItems().with(Collector.of(Buffer::buffer, Buffer::appendByte, (b1, b2) -> b1));
        } else {
            return Uni.createFrom().failure(new IllegalArgumentException("Attachment has no data"));
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void configure(Optional<String> from, Optional<String> bounceAddress, boolean mock) {
        this.from = from.orElse(null);
        this.bounceAddress = bounceAddress.orElse(null);
        this.mock = mock;
    }
}
