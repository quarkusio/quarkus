package io.quarkus.mailer.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;

/**
 * Mock mailbox bean, will be populated if mocking emails.
 */
@ApplicationScoped
public class MockMailboxImpl implements MockMailbox {

    private Map<String, List<Mail>> sentMessages = new HashMap<>();
    private int sentMessagesCount;

    CompletionStage<Void> send(Mail email) {
        if (email.getTo() != null) {
            for (String to : email.getTo()) {
                send(email, to);
            }
        }
        if (email.getCc() != null) {
            for (String to : email.getCc()) {
                send(email, to);
            }
        }
        if (email.getBcc() != null) {
            for (String to : email.getBcc()) {
                send(email, to);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private void send(Mail sentMail, String to) {
        List<Mail> mails = sentMessages.get(to);
        if (mails == null) {
            mails = new LinkedList<>();
            sentMessages.put(to, mails);
        }
        sentMessagesCount++;
        mails.add(sentMail);
    }

    @Override
    public List<Mail> getMessagesSentTo(String address) {
        return sentMessages.get(address);
    }

    @Override
    public void clear() {
        sentMessagesCount = 0;
        sentMessages.clear();
    }

    @Override
    public int getTotalMessagesSent() {
        return sentMessagesCount;
    }
}
