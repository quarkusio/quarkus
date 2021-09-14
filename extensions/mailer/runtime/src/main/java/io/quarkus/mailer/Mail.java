package io.quarkus.mailer;

import java.io.File;
import java.util.*;

import org.reactivestreams.Publisher;

/**
 * Represents an e-mail.
 * This class encapsulates the various attributes you want to set on an e-mail you are going to send (to, subject,
 * body...).
 * <p>
 * Instances are NOT thread-safe.
 */
public class Mail {

    private List<String> bcc = new ArrayList<>();
    private List<String> cc = new ArrayList<>();
    private String from;
    private List<String> replyTo = new ArrayList<>();
    private String bounceAddress;
    private String subject;
    private String text;
    private String html;
    private List<String> to = new ArrayList<>();

    private Map<String, List<String>> headers = new HashMap<>();
    private List<Attachment> attachments = new ArrayList<>();

    /**
     * Creates a new instance of {@link Mail}.
     */
    public Mail() {
        // empty
    }

    /**
     * Creates a new instance of {@link Mail} that contains a "text" body.
     * The returned instance can be modified.
     *
     * @param to the address of the recipient
     * @param subject the subject
     * @param text the body
     * @return the new {@link Mail} instance.
     */
    public static Mail withText(String to, String subject, String text) {
        return new Mail().addTo(to).setSubject(subject).setText(text);
    }

    /**
     * Creates a new instance of {@link Mail} that contains a "html" body.
     * The returned instance can be modified.
     *
     * @param to the address of the recipient
     * @param subject the subject
     * @param html the body
     * @return the new {@link Mail} instance.
     */
    public static Mail withHtml(String to, String subject, String html) {
        return new Mail().addTo(to).setSubject(subject).setHtml(html);
    }

    /**
     * Adds BCC recipients.
     *
     * @param bcc the recipients, each item must be a valid email address.
     * @return the current {@link Mail}
     */
    public Mail addBcc(String... bcc) {
        if (bcc != null) {
            Collections.addAll(this.bcc, bcc);
        }
        return this;
    }

    /**
     * Adds CC recipients.
     *
     * @param cc the recipients, each item must be a valid email address.
     * @return the current {@link Mail}
     */
    public Mail addCc(String... cc) {
        if (cc != null) {
            Collections.addAll(this.cc, cc);
        }
        return this;
    }

    /**
     * Adds TO recipients.
     *
     * @param to the recipients, each item must be a valid email address.
     * @return the current {@link Mail}
     */
    public Mail addTo(String... to) {
        if (to != null) {
            Collections.addAll(this.to, to);
        }
        return this;
    }

    /**
     * @return the BCC recipients.
     */
    public List<String> getBcc() {
        return bcc;
    }

    /**
     * Sets the BCC recipients.
     *
     * @param bcc the list of recipients
     * @return the current {@link Mail}
     */
    public Mail setBcc(List<String> bcc) {
        if (bcc == null) {
            this.bcc = new ArrayList<>();
        } else {
            this.bcc = bcc;
        }
        return this;
    }

    /**
     * @return the CC recipients.
     */
    public List<String> getCc() {
        return cc;
    }

    /**
     * Sets the CC recipients.
     *
     * @param cc the list of recipients
     * @return the current {@link Mail}
     */
    public Mail setCc(List<String> cc) {
        if (cc == null) {
            this.cc = new ArrayList<>();
        } else {
            this.cc = cc;
        }
        return this;
    }

    /**
     * @return the sender address.
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the sender address. Notes that it's not accepted to send an email without a sender address.
     * A default sender address can be configured in the application properties ( {@code quarkus.mailer.from} )
     *
     * @param from the sender address
     * @return the current {@link Mail}
     */
    public Mail setFrom(String from) {
        this.from = from;
        return this;
    }

    /**
     * @return the reply-to address. In the case of multiple addresses, the comma-separated list is returned, following
     *         the https://datatracker.ietf.org/doc/html/rfc5322#section-3.6.2 recommendation. If no reply-to address has been
     *         set, it returns {@code null}.
     */
    public String getReplyTo() {
        if (replyTo == null || replyTo.isEmpty()) {
            return null;
        }
        return String.join(",", replyTo);
    }

    /**
     * Adds a reply-to address.
     *
     * @param replyTo the address to use as reply-to. Must be a valid email address.
     * @return the current {@link Mail}
     * @see #setReplyTo(String)
     */
    public Mail addReplyTo(String replyTo) {
        this.replyTo.add(replyTo);
        return this;
    }

    /**
     * Sets the reply-to address.
     *
     * @param replyTo the address to use as reply-to. Must be a valid email address.
     * @return the current {@link Mail}
     * @see #setReplyTo(String[])
     */
    public Mail setReplyTo(String replyTo) {
        this.replyTo.clear();
        this.replyTo.add(replyTo);
        return this;
    }

    /**
     * Sets the reply-to addresses.
     *
     * @param replyTo the addresses to use as reply-to. Must contain valid email addresses, must contain at least
     *        one address.
     * @return the current {@link Mail}
     */
    public Mail setReplyTo(String... replyTo) {
        this.replyTo.clear();
        Collections.addAll(this.replyTo, replyTo);
        return this;
    }

    /**
     * @return the bounce address.
     */
    public String getBounceAddress() {
        return bounceAddress;
    }

    /**
     * Sets the bounce address.
     * A default sender address can be configured in the application properties ( {@code quarkus.mailer.bounceAddress} )
     *
     * @param bounceAddress the bounce address, must be a valid email address.
     * @return the current {@link Mail}
     */
    public Mail setBounceAddress(String bounceAddress) {
        this.bounceAddress = bounceAddress;
        return this;
    }

    /**
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the email subject.
     *
     * @param subject the subject
     * @return the current {@link Mail}
     */
    public Mail setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * @return the text content of the email
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the body of the email as plain text.
     *
     * @param text the content
     * @return the current {@link Mail}
     */
    public Mail setText(String text) {
        this.text = text;
        return this;
    }

    /**
     * @return the HTML content of the email
     */
    public String getHtml() {
        return html;
    }

    /**
     * Sets the body of the email as HTML.
     *
     * @param html the content
     * @return the current {@link Mail}
     */
    public Mail setHtml(String html) {
        this.html = html;
        return this;
    }

    /**
     * @return the TO recipients.
     */
    public List<String> getTo() {
        return to;
    }

    /**
     * Sets the TO recipients.
     *
     * @param to the list of recipients
     * @return the current {@link Mail}
     */
    public Mail setTo(List<String> to) {
        if (to == null) {
            this.to = new ArrayList<>();
        } else {
            this.to = to;
        }
        return this;
    }

    /**
     * @return the current set of headers.
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Adds a header value. If this header already has a value, the value is appended.
     *
     * @param key the header name, must not be {@code null}
     * @param values the header values, must not be {@code null}
     * @return the current {@link Mail}
     */
    public Mail addHeader(String key, String... values) {
        if (key == null || values == null) {
            throw new IllegalArgumentException("Cannot add header, key and value must not be null");
        }
        List<String> content = this.headers.computeIfAbsent(key, k -> new ArrayList<>());
        Collections.addAll(content, values);
        return this;
    }

    /**
     * Removes a header.
     *
     * @param key the header name, must not be {@code null}.
     * @return the current {@link Mail}
     */
    public Mail removeHeader(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Cannot remove header, key must not be null");
        }
        headers.remove(key);
        return this;
    }

    /**
     * Sets the list of headers.
     *
     * @param headers the headers
     * @return the current {@link Mail}
     */
    public Mail setHeaders(Map<String, List<String>> headers) {
        if (headers == null) {
            this.headers = new HashMap<>();
        } else {
            this.headers = headers;
        }
        return this;
    }

    /**
     * Adds an inline attachment.
     *
     * @param name the name of the attachment, generally a file name.
     * @param file the file to be attached. Note that the file will be read asynchronously.
     * @param contentType the content type
     * @param contentId the content id. It must follows the {@code <some-id@some-domain>} syntax. Then the HTML
     *        content can reference this attachment using {@code src="cid:some-id@some-domain"}.
     * @return the current {@link Mail}
     */
    public Mail addInlineAttachment(String name, File file, String contentType, String contentId) {
        this.attachments.add(new Attachment(name, file, contentType, contentId));
        return this;
    }

    /**
     * Adds an attachment.
     *
     * @param name the name of the attachment, generally a file name.
     * @param file the file to be attached. Note that the file will be read asynchronously.
     * @param contentType the content type.
     * @return the current {@link Mail}
     */
    public Mail addAttachment(String name, File file, String contentType) {
        this.attachments.add(new Attachment(name, file, contentType));
        return this;
    }

    /**
     * Adds an attachment.
     *
     * @param name the name of the attachment, generally a file name.
     * @param data the binary data to be attached
     * @param contentType the content type.
     * @return the current {@link Mail}
     */
    public Mail addAttachment(String name, byte[] data, String contentType) {
        this.attachments.add(new Attachment(name, data, contentType));
        return this;
    }

    /**
     * Adds an attachment.
     *
     * @param name the name of the attachment, generally a file name.
     * @param data the binary data to be attached
     * @param contentType the content type.
     * @return the current {@link Mail}
     */
    public Mail addAttachment(String name, Publisher<Byte> data, String contentType) {
        this.attachments.add(new Attachment(name, data, contentType));
        return this;
    }

    /**
     * Adds an inline attachment.
     *
     * @param name the name of the attachment, generally a file name.
     * @param data the binary data to be attached
     * @param contentType the content type
     * @param contentId the content id. It must follows the {@code <some-id@some-domain>} syntax. Then the HTML
     *        content can reference this attachment using {@code src="cid:some-id@some-domain"}.
     * @return the current {@link Mail}
     */
    public Mail addInlineAttachment(String name, byte[] data, String contentType, String contentId) {
        this.attachments.add(new Attachment(name, data, contentType, contentId));
        return this;
    }

    /**
     * Adds an inline attachment.
     *
     * @param name the name of the attachment, generally a file name.
     * @param data the binary data to be attached
     * @param contentType the content type
     * @param contentId the content id. It must follows the {@code <some-id@some-domain>} syntax. Then the HTML
     *        content can reference this attachment using {@code src="cid:some-id@some-domain"}.
     * @return the current {@link Mail}
     */
    public Mail addInlineAttachment(String name, Publisher<Byte> data, String contentType, String contentId) {
        this.attachments.add(new Attachment(name, data, contentType, contentId));
        return this;
    }

    /**
     * Adds an attachment.
     *
     * @param name the name of the attachment, generally a file name.
     * @param data the binary data to be attached
     * @param contentType the content type
     * @param description the description of the attachment
     * @param disposition the disposition of the attachment
     * @return the current {@link Mail}
     */
    public Mail addAttachment(String name, byte[] data, String contentType, String description, String disposition) {
        this.attachments.add(new Attachment(name, data, contentType, description, disposition));
        return this;
    }

    /**
     * Adds an attachment.
     *
     * @param name the name of the attachment, generally a file name.
     * @param data the binary data to be attached
     * @param contentType the content type
     * @param description the description of the attachment
     * @param disposition the disposition of the attachment
     * @return the current {@link Mail}
     */
    public Mail addAttachment(String name, Publisher<Byte> data, String contentType, String description,
            String disposition) {
        this.attachments.add(new Attachment(name, data, contentType, description, disposition));
        return this;
    }

    /**
     * @return the list of attachments
     */
    public List<Attachment> getAttachments() {
        return attachments;
    }

    /**
     * Sets the attachment list.
     *
     * @param attachments the attachments.
     * @return the current {@link Mail}
     */
    public Mail setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
        return this;
    }
}
