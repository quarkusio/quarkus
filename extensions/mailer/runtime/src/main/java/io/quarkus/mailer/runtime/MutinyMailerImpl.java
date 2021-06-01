package io.quarkus.mailer.runtime;

import static java.util.Arrays.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.reactivestreams.Publisher;

import io.quarkus.mailer.Attachment;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
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

    @Inject
    MailerSupport mailerSupport;

    @Override
    public Uni<Void> send(Mail... mails) {
        if (mails == null) {
            throw new IllegalArgumentException("The `mails` parameter must not be `null`");
        }

        List<Uni<Void>> unis = stream(mails)
                .map(new Function<Mail, Uni<Void>>() {
                    @Override
                    public Uni<Void> apply(Mail mail) {
                        return MutinyMailerImpl.this.toMailMessage(mail)
                                .chain(new Function<MailMessage, Uni<? extends Void>>() {
                                    @Override
                                    public Uni<? extends Void> apply(MailMessage mailMessage) {
                                        return send(mail, mailMessage);
                                    }
                                });
                    }
                })
                .collect(Collectors.toList());

        return Uni.combine().all().unis(unis).discardItems();
    }

    private Uni<Void> send(Mail mail, MailMessage message) {
        if (mailerSupport.isMock()) {
            LOGGER.infof("Sending email %s from %s to %s, text body: \n%s\nhtml body: \n%s",
                    message.getSubject(), message.getFrom(), message.getTo(),
                    message.getText() == null ? "<empty>" : message.getText(),
                    message.getHtml() == null ? "<empty>" : message.getHtml());
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
            message.setBounceAddress(this.mailerSupport.getBounceAddress());
        }

        if (mail.getFrom() != null) {
            message.setFrom(mail.getFrom());
        } else {
            message.setFrom(this.mailerSupport.getFrom());
        }
        message.setTo(mail.getTo());
        message.setCc(mail.getCc());
        message.setBcc(mail.getBcc());
        message.setSubject(mail.getSubject());
        message.setText(mail.getText());
        message.setHtml(mail.getHtml());
        message.setHeaders(toMultimap(mail.getHeaders()));
        if (mail.getReplyTo() != null) {
            // getReplyTo produces the comma-separated list.
            message.addHeader("Reply-To", mail.getReplyTo());
        }

        List<Uni<?>> stages = new ArrayList<>();
        List<MailAttachment> attachments = new CopyOnWriteArrayList<>();
        List<MailAttachment> inline = new CopyOnWriteArrayList<>();
        for (Attachment attachment : mail.getAttachments()) {
            if (attachment.isInlineAttachment()) {
                stages.add(
                        toMailAttachment(attachment).onItem().invoke(inline::add));
            } else {
                stages.add(
                        toMailAttachment(attachment).onItem().invoke(attachments::add));
            }
        }

        if (stages.isEmpty()) {
            message.setAttachment(attachments);
            message.setInlineAttachment(inline);
            return Uni.createFrom().item(message);
        }

        return Uni.combine().all().unis(stages).combinedWith(res -> {
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
        MailAttachment attach = MailAttachment.create();
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
                .onItem().transform(attach::setData);
    }

    public static Uni<Buffer> getAttachmentStream(Vertx vertx, Attachment attachment) {
        if (attachment.getFile() != null) {
            Uni<AsyncFile> open = vertx.fileSystem().open(attachment.getFile().getAbsolutePath(),
                    new OpenOptions().setRead(true).setCreate(false));
            return open
                    .flatMap(af -> af.toMulti()
                            .map(io.vertx.mutiny.core.buffer.Buffer::getDelegate)
                            .onTermination().call((r, f) -> af.close())
                            .collect().in(Buffer::buffer, Buffer::appendBuffer));
        } else if (attachment.getData() != null) {
            Publisher<Byte> data = attachment.getData();
            return Multi.createFrom().publisher(data)
                    .collect().in(Buffer::buffer, Buffer::appendByte);
        } else {
            return Uni.createFrom().failure(new IllegalArgumentException("Attachment has no data"));
        }
    }
}
