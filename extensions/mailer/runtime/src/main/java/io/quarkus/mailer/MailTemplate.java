package io.quarkus.mailer;

import java.util.concurrent.CompletionStage;

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

        MailTemplateInstance bounceAddress(String bounceAddress);

        MailTemplateInstance data(String key, Object value);

        CompletionStage<Void> send();

    }

}
