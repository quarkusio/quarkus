package io.quarkus.mailer;

import java.util.List;

/**
 * Mock mail collector, will be populated if mocking emails.
 */
public interface MockMailbox {

    /**
     * Returns a list of mails sent to the given address, whether it was via To, Cc or Bcc.
     * 
     * @param address the email address we want to retrieve mail from
     * @return a list of messages sent to the given address, possibly empty.
     */
    List<Mail> getMessagesSentTo(String address);

    /**
     * Removes every sent message.
     */
    void clear();

    /**
     * Gets the total number of messages sent. This counts every message sent to every recipient.
     * 
     * @return the total number of messages sent.
     */
    int getTotalMessagesSent();
}
