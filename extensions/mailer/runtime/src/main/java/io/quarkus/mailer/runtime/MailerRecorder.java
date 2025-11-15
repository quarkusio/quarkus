package io.quarkus.mailer.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.ext.mail.MailClient;

@Recorder
public class MailerRecorder {

    public Supplier<MailerSupport> mailerSupportSupplier(MailerSupport mailerSupport) {
        return new Supplier<MailerSupport>() {
            @Override
            public MailerSupport get() {
                return mailerSupport;
            }
        };
    }

    public Function<SyntheticCreationalContext<MailClient>, MailClient> mailClientFunction(String name) {
        return new Function<SyntheticCreationalContext<MailClient>, MailClient>() {
            @Override
            public MailClient apply(SyntheticCreationalContext<MailClient> context) {
                return context.getInjectedReference(Mailers.class).mailClientFromName(name);
            }
        };
    }

    public Function<SyntheticCreationalContext<io.vertx.mutiny.ext.mail.MailClient>, io.vertx.mutiny.ext.mail.MailClient> reactiveMailClientFunction(
            String name) {
        return new Function<SyntheticCreationalContext<io.vertx.mutiny.ext.mail.MailClient>, io.vertx.mutiny.ext.mail.MailClient>() {
            @Override
            public io.vertx.mutiny.ext.mail.MailClient apply(
                    SyntheticCreationalContext<io.vertx.mutiny.ext.mail.MailClient> context) {
                return context.getInjectedReference(Mailers.class).reactiveMailClientFromName(name);
            }
        };
    }

    public Function<SyntheticCreationalContext<Mailer>, Mailer> mailerFunction(String name) {
        return new Function<SyntheticCreationalContext<Mailer>, Mailer>() {
            @Override
            public Mailer apply(SyntheticCreationalContext<Mailer> context) {
                return context.getInjectedReference(Mailers.class).mailerFromName(name);
            }
        };
    }

    public Function<SyntheticCreationalContext<ReactiveMailer>, ReactiveMailer> reactiveMailerFunction(String name) {
        return new Function<SyntheticCreationalContext<ReactiveMailer>, ReactiveMailer>() {
            @Override
            public ReactiveMailer apply(SyntheticCreationalContext<ReactiveMailer> context) {
                return context.getInjectedReference(Mailers.class).reactiveMailerFromName(name);
            }
        };
    }

    public Function<SyntheticCreationalContext<MockMailbox>, MockMailbox> mockMailboxFunction(String name) {
        return new Function<SyntheticCreationalContext<MockMailbox>, MockMailbox>() {
            @Override
            public MockMailbox apply(SyntheticCreationalContext<MockMailbox> context) {
                return context.getInjectedReference(Mailers.class).mockMailboxFromName(name);
            }
        };
    }
}
