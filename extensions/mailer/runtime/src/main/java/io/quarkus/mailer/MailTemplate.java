package io.quarkus.mailer;

import java.io.File;

import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;

/**
 * Represents an e-mail definition based on a template.
 */
public interface MailTemplate {

    /**
     *
     * @return a new template instance
     */
    MailTemplateInstance instance();

    default MailTemplateInstance of(Mail mail) {
        return instance().mail(mail);
    }

    default MailTemplateInstance to(String... values) {
        return instance().to(values);
    }

    default MailTemplateInstance data(String key, Object value) {
        return instance().data(key, value);
    }

    /**
     * Represents an instance of {@link MailTemplate}.
     * <p>
     * This construct is not thread-safe.
     */
    interface MailTemplateInstance {

        default MailTemplateInstance mail(Mail mail) {
            throw new UnsupportedOperationException();
        }

        default MailTemplateInstance to(String... to) {
            throw new UnsupportedOperationException();
        }

        default MailTemplateInstance cc(String... cc) {
            throw new UnsupportedOperationException();
        }

        default MailTemplateInstance bcc(String... bcc) {
            throw new UnsupportedOperationException();
        }

        default MailTemplateInstance subject(String subject) {
            throw new UnsupportedOperationException();
        }

        default MailTemplateInstance from(String from) {
            throw new UnsupportedOperationException();
        }

        default MailTemplateInstance replyTo(String replyTo) {
            throw new UnsupportedOperationException();
        }

        default MailTemplateInstance replyTo(String... replyTo) {
            throw new UnsupportedOperationException();
        }

        default MailTemplateInstance bounceAddress(String bounceAddress) {
            throw new UnsupportedOperationException();
        }

        default MailTemplateInstance addInlineAttachment(String name, File file, String contentType, String contentId) {
            throw new UnsupportedOperationException();
        }

        default MailTemplateInstance addInlineAttachment(String name, byte[] data, String contentType, String contentId) {
            throw new UnsupportedOperationException();
        }

        default MailTemplateInstance addAttachment(String name, File file, String contentType) {
            throw new UnsupportedOperationException();
        }

        default MailTemplateInstance addAttachment(String name, byte[] data, String contentType) {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @param key
         * @param value
         * @return self
         * @see io.quarkus.qute.TemplateInstance#data(String, Object)
         */
        default MailTemplateInstance data(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @param key
         * @param value
         * @return self
         * @see io.quarkus.qute.TemplateInstance#setAttribute(String, Object)
         */
        default MailTemplateInstance setAttribute(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        /**
         * Sends all e-mail definitions based on available template variants, i.e. {@code text/html} and {@code text/plain}
         * template variants.
         *
         * @return a {@link Uni} indicating when the mails have been sent
         * @see ReactiveMailer#send(Mail...)
         */
        @CheckReturnValue
        default Uni<Void> send() {
            throw new UnsupportedOperationException();
        }

        /**
         * Sends all e-mail definitions and blocks the current thread while waiting for the result.
         *
         * @see #send()
         */
        default void sendAndAwait() {
            send().await().indefinitely();
        }

        /**
         * The returned instance does not represent a specific template but a delegating template.
         * <p>
         * You can select the corresponding variant via {@link TemplateInstance#setAttribute(String, Object)} where the
         * attribute key is {@link TemplateInstance#SELECTED_VARIANT}. If no variant is selected, the default instance is used.
         *
         * @return the underlying template instance
         */
        default TemplateInstance templateInstance() {
            throw new UnsupportedOperationException();
        }

    }

}
