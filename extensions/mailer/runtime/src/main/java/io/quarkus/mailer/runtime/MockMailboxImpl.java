package io.quarkus.mailer.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.mailencoder.EmailAddress;

/**
 * Mock mailbox bean, will be populated if mocking emails.
 */
public class MockMailboxImpl implements MockMailbox {

    private Map<String, List<Mail>> sentMails = new HashMap<>();
    private Map<String, List<MailMessage>> sentMailMessages = new HashMap<>();
    private int sentMessagesCount;

    Uni<Void> send(Mail email, MailMessage mailMessage) {
        if (email.getTo() != null) {
            for (String to : email.getTo()) {
                validateEmailAddress(to);
                send(email, mailMessage, to);
            }
        }
        if (email.getCc() != null) {
            for (String to : email.getCc()) {
                validateEmailAddress(to);
                send(email, mailMessage, to);
            }
        }
        if (email.getBcc() != null) {
            for (String to : email.getBcc()) {
                validateEmailAddress(to);
                send(email, mailMessage, to);
            }
        }
        return Uni.createFrom().item(() -> null);
    }

    private void validateEmailAddress(String to) {
        // Just here to validate the email address.
        new EmailAddress(to);
    }

    private void send(Mail sentMail, MailMessage sentMailMessage, String to) {
        sentMails.computeIfAbsent(to, k -> new ArrayList<>()).add(sentMail);
        sentMailMessages.computeIfAbsent(to, k -> new ArrayList<>()).add(sentMailMessage);
        sentMessagesCount++;
    }

    @Override
    public List<Mail> getMessagesSentTo(String address) {
        return sentMails.getOrDefault(address, List.of());
    }

    @Override
    public List<Mail> getMailsSentTo(String address) {
        return sentMails.getOrDefault(address, List.of());
    }

    @Override
    public List<MailMessage> getMailMessagesSentTo(String address) {
        return sentMailMessages.getOrDefault(address, List.of());
    }

    @Override
    public void clear() {
        sentMessagesCount = 0;
        sentMails.clear();
        sentMailMessages.clear();
    }

    @Override
    public int getTotalMessagesSent() {
        return sentMessagesCount;
    }
}
