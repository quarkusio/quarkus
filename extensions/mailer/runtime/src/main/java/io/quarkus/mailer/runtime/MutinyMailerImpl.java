package io.quarkus.mailer.runtime;

import static java.util.Arrays.stream;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

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

public class MutinyMailerImpl implements ReactiveMailer {

    private static final Logger LOGGER = Logger.getLogger("quarkus-mailer");

    private final Vertx vertx;

    private final MailClient client;

    private final MockMailboxImpl mockMailbox;

    private final String from;

    private final String bounceAddress;

    private final boolean mock;

    private final List<Pattern> approvedRecipients;

    private boolean logRejectedRecipients;

    MutinyMailerImpl(Vertx vertx, MailClient client, MockMailboxImpl mockMailbox,
            String from, String bounceAddress, boolean mock, List<Pattern> approvedRecipients,
            boolean logRejectedRecipients) {
        this.vertx = vertx;
        this.client = client;
        this.mockMailbox = mockMailbox;
        this.from = from;
        this.bounceAddress = bounceAddress;
        this.mock = mock;
        this.approvedRecipients = approvedRecipients;
        this.logRejectedRecipients = logRejectedRecipients;
    }

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
        if (!approvedRecipients.isEmpty()) {
            Recipients to = filterApprovedRecipients(message.getTo());
            Recipients cc = filterApprovedRecipients(message.getCc());
            Recipients bcc = filterApprovedRecipients(message.getBcc());

            if (to.approved.isEmpty() && cc.approved.isEmpty() && bcc.approved.isEmpty()) {
                logRejectedRecipients("Email '%s' was not sent because all recipients were rejected by the configuration: %s",
                        message.getSubject(), to.rejected, cc.rejected, bcc.rejected);
                return Uni.createFrom().voidItem();
            }

            if (!to.rejected.isEmpty() || !cc.rejected.isEmpty() || !bcc.rejected.isEmpty()) {
                logRejectedRecipients(
                        "Email '%s' was not sent to the following recipients as they were rejected by the configuration: %s",
                        message.getSubject(), to.rejected, cc.rejected, bcc.rejected);
            }

            if (!to.rejected.isEmpty()) {
                mail.setTo(to.approved);
                message.setTo(to.approved);
            }
            if (!cc.rejected.isEmpty()) {
                mail.setCc(cc.approved);
                message.setCc(cc.approved);
            }
            if (!bcc.rejected.isEmpty()) {
                mail.setBcc(bcc.approved);
                message.setBcc(bcc.approved);
            }
        }

        if (mock) {
            LOGGER.infof("Sending email %s from %s to %s (cc: %s, bcc: %s), text body: \n%s\nhtml body: \n%s",
                    message.getSubject(), message.getFrom(), message.getTo(),
                    message.getCc(), message.getBcc(),
                    message.getText() == null ? "<empty>" : message.getText(),
                    message.getHtml() == null ? "<empty>" : message.getHtml());
            return mockMailbox.send(mail, message);
        } else {
            return client.sendMail(message)
                    .replaceWithVoid();
        }
    }

    private Uni<MailMessage> toMailMessage(Mail mail) {
        MailMessage message = new MailMessage();

        if (mail.getBounceAddress() != null) {
            message.setBounceAddress(mail.getBounceAddress());
        } else {
            message.setBounceAddress(bounceAddress);
        }

        if (mail.getFrom() != null) {
            message.setFrom(mail.getFrom());
        } else {
            message.setFrom(from);
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

    private Recipients filterApprovedRecipients(List<String> emails) {
        if (approvedRecipients.isEmpty()) {
            return new Recipients(emails, List.of());
        }

        List<String> allowedRecipients = new ArrayList<>();
        List<String> rejectedRecipients = new ArrayList<>();

        emailLoop: for (String email : emails) {
            for (Pattern approvedRecipient : approvedRecipients) {
                if (approvedRecipient.matcher(email).matches()) {
                    allowedRecipients.add(email);
                    continue emailLoop;
                }
            }

            rejectedRecipients.add(email);
        }

        return new Recipients(allowedRecipients, rejectedRecipients);
    }

    @SafeVarargs
    private void logRejectedRecipients(String logMessage, String subject, List<String>... rejectedRecipientLists) {
        if (logRejectedRecipients) {
            Set<String> allRejectedRecipients = new LinkedHashSet<>();
            for (List<String> rejectedRecipients : rejectedRecipientLists) {
                allRejectedRecipients.addAll(rejectedRecipients);
            }

            LOGGER.warn(String.format(logMessage, subject, allRejectedRecipients));
        } else if (LOGGER.isDebugEnabled()) {
            List<String> allRejectedRecipients = new ArrayList<>();
            for (List<String> rejectedRecipients : rejectedRecipientLists) {
                allRejectedRecipients.addAll(rejectedRecipients);
            }

            LOGGER.warn(String.format(logMessage, subject, allRejectedRecipients));
        }
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

    private static class Recipients {

        private final List<String> approved;
        private final List<String> rejected;

        Recipients(List<String> approved, List<String> rejected) {
            this.approved = approved;
            this.rejected = rejected;
        }
    }
}
