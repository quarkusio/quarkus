package io.quarkus.mailer.runtime;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.vertx.ext.mail.MailClient;

public final class MailerBeanCreators {

    public static final String MAILER_NAME_PARAM = "mailerName";

    private MailerBeanCreators() {
    }

    private static Mailers mailers(SyntheticCreationalContext<?> context) {
        return context.getInjectedReference(Mailers.class);
    }

    private static String name(SyntheticCreationalContext<?> context) {
        return (String) context.getParams().get(MAILER_NAME_PARAM);
    }

    public static class MailClientCreator implements BeanCreator<MailClient> {
        @Override
        public MailClient create(SyntheticCreationalContext<MailClient> context) {
            return mailers(context).mailClientFromName(name(context));
        }
    }

    public static class ReactiveMailClientCreator implements BeanCreator<io.vertx.mutiny.ext.mail.MailClient> {
        @Override
        public io.vertx.mutiny.ext.mail.MailClient create(
                SyntheticCreationalContext<io.vertx.mutiny.ext.mail.MailClient> context) {
            return mailers(context).reactiveMailClientFromName(name(context));
        }
    }

    public static class MailerCreator implements BeanCreator<Mailer> {
        @Override
        public Mailer create(SyntheticCreationalContext<Mailer> context) {
            return mailers(context).mailerFromName(name(context));
        }
    }

    public static class ReactiveMailerCreator implements BeanCreator<ReactiveMailer> {
        @Override
        public ReactiveMailer create(SyntheticCreationalContext<ReactiveMailer> context) {
            return mailers(context).reactiveMailerFromName(name(context));
        }
    }

    public static class MockMailboxCreator implements BeanCreator<MockMailbox> {
        @Override
        public MockMailbox create(SyntheticCreationalContext<MockMailbox> context) {
            return mailers(context).mockMailboxFromName(name(context));
        }
    }
}
