package io.quarkus.mailer;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

/**
 * Represents a sent mail.
 * Instances of this class are sent using CDI events.
 *
 * @param from the sender address
 * @param to the list of recipients
 * @param cc the list of CC recipients
 * @param bcc the list of BCC recipients
 * @param replyTo the list of reply-to addresses
 * @param bounceAddress the bounce address
 * @param subject the subject
 * @param textBody the text body
 * @param htmlBody the HTML body
 * @param headers the headers
 * @param attachments the attachments
 */
public record SentMail(String from,
        List<String> to, List<String> cc, List<String> bcc,
        String replyTo, String bounceAddress,
        String subject, String textBody, String htmlBody,
        Map<String, List<String>> headers, List<SentAttachment> attachments) {

    /**
     * An immutable representation of an attachment that has been sent.
     *
     * @param name the name
     * @param file the file
     * @param description the description
     * @param disposition the disposition
     * @param data the data
     * @param contentType the content type
     * @param contentId the content ID
     */
    public record SentAttachment(String name, File file, String description, String disposition,
            Flow.Publisher<Byte> data, String contentType, String contentId) {
    }
}
