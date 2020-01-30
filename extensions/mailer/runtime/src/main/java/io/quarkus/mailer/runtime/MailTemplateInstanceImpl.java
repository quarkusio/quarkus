package io.quarkus.mailer.runtime;

import static io.quarkus.qute.api.VariantTemplate.SELECTED_VARIANT;
import static io.quarkus.qute.api.VariantTemplate.VARIANTS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MailTemplate;
import io.quarkus.mailer.MailTemplate.MailTemplateInstance;
import io.quarkus.mailer.ReactiveMailer;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.quarkus.qute.api.VariantTemplate;

class MailTemplateInstanceImpl implements MailTemplate.MailTemplateInstance {

    private final ReactiveMailer mailer;
    private final TemplateInstance templateInstance;
    private final Map<String, Object> data;
    private Mail mail;

    MailTemplateInstanceImpl(ReactiveMailer mailer, TemplateInstance templateInstance) {
        this.mailer = mailer;
        this.templateInstance = templateInstance;
        this.data = new HashMap<>();
        this.mail = new Mail();
    }

    @Override
    public MailTemplateInstance mail(Mail mail) {
        this.mail = mail;
        return this;
    }

    @Override
    public MailTemplateInstance to(String... to) {
        this.mail.addTo(to);
        return this;
    }

    @Override
    public MailTemplateInstance cc(String... cc) {
        this.mail.addCc(cc);
        return this;
    }

    @Override
    public MailTemplateInstance bcc(String... bcc) {
        this.mail.addBcc(bcc);
        return this;
    }

    @Override
    public MailTemplateInstance subject(String subject) {
        this.mail.setSubject(subject);
        return this;
    }

    @Override
    public MailTemplateInstance from(String from) {
        this.mail.setFrom(from);
        return this;
    }

    @Override
    public MailTemplateInstance replyTo(String replyTo) {
        this.mail.setReplyTo(replyTo);
        return this;
    }

    @Override
    public MailTemplateInstance bounceAddress(String bounceAddress) {
        this.mail.setBounceAddress(bounceAddress);
        return this;
    }

    @Override
    public MailTemplateInstance data(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    @Override
    public CompletionStage<Void> send() {

        CompletableFuture<Void> result = new CompletableFuture<>();

        if (templateInstance.getAttribute(VariantTemplate.VARIANTS) != null) {

            List<Result> results = new ArrayList<>();

            @SuppressWarnings("unchecked")
            List<Variant> variants = (List<Variant>) templateInstance.getAttribute(VARIANTS);
            for (Variant variant : variants) {
                if (variant.mediaType.equals(Variant.TEXT_HTML) || variant.mediaType.equals(Variant.TEXT_PLAIN)) {
                    results.add(new Result(variant,
                            templateInstance.setAttribute(SELECTED_VARIANT, variant).data(data).renderAsync()
                                    .toCompletableFuture()));
                }
            }

            if (results.isEmpty()) {
                throw new IllegalStateException("No suitable template variant found");
            }

            CompletableFuture<Void> all = CompletableFuture
                    .allOf(results.stream().map(Result::getValue).toArray(CompletableFuture[]::new));
            all.whenComplete((r1, t1) -> {
                if (t1 != null) {
                    result.completeExceptionally(t1);
                } else {
                    try {
                        for (Result res : results) {
                            if (res.variant.mediaType.equals(Variant.TEXT_HTML)) {
                                mail.setHtml(res.value.get());
                            } else if (res.variant.mediaType.equals(Variant.TEXT_PLAIN)) {
                                mail.setText(res.value.get());
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        result.completeExceptionally(e);
                    }
                    mailer.send(mail).whenComplete((r, t) -> {
                        if (t != null) {
                            result.completeExceptionally(t);
                        } else {
                            result.complete(null);
                        }
                    });
                }
            });
        } else {
            throw new IllegalStateException("No template variant found");
        }
        return result;
    }

    static class Result {

        final Variant variant;
        final CompletableFuture<String> value;

        public Result(Variant variant, CompletableFuture<String> result) {
            this.variant = variant;
            this.value = result;
        }

        CompletableFuture<String> getValue() {
            return value;
        }
    }

}
