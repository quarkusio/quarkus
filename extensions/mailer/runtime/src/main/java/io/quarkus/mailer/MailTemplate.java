package io.quarkus.mailer;

import java.io.File;

import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.qute.TemplateInstance;
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

        MailTemplateInstance mail(Mail mail);

        MailTemplateInstance to(String... to);

        MailTemplateInstance cc(String... cc);

        MailTemplateInstance bcc(String... bcc);

        MailTemplateInstance subject(String subject);

        MailTemplateInstance from(String from);

        MailTemplateInstance replyTo(String replyTo);

        MailTemplateInstance replyTo(String... replyTo);

        MailTemplateInstance bounceAddress(String bounceAddress);

        MailTemplateInstance addInlineAttachment(String name, File file, String contentType, String contentId);

        /**
         * 
         * @param key
         * @param value
         * @return self
         * @see io.quarkus.qute.TemplateInstance#data(String, Object)
         */
        MailTemplateInstance data(String key, Object value);

        /**
         * 
         * @param key
         * @param value
         * @return self
         * @see io.quarkus.qute.TemplateInstance#setAttribute(String, Object)
         */
        MailTemplateInstance setAttribute(String key, Object value);

        /**
         * Sends all e-mail definitions based on available template variants, i.e. {@code text/html} and {@code text/plain}
         * template variants.
         * 
         * @return a {@link Uni} indicating when the mails have been sent
         * @see ReactiveMailer#send(Mail...)
         */
        Uni<Void> send();

        /**
         * The returned instance does not represent a specific template but a delegating template.
         * <p>
         * You can select the corresponding variant via {@link TemplateInstance#setAttribute(String, Object)} where the
         * attribute key is {@link TemplateInstance#SELECTED_VARIANT}. If no variant is selected, the default instance is used.
         * 
         * @return the underlying template instance
         */
        TemplateInstance templateInstance();

    }

}
