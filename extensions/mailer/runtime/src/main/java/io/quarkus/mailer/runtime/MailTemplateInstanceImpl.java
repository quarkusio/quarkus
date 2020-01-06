package io.quarkus.mailer.runtime;

import static io.quarkus.qute.api.VariantTemplate.SELECTED_VARIANT;
import static io.quarkus.qute.api.VariantTemplate.VARIANTS;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MailTemplate;
import io.quarkus.mailer.MailTemplate.MailTemplateInstance;
import io.quarkus.mailer.mutiny.ReactiveMailer;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.quarkus.qute.api.VariantTemplate;
import io.smallrye.mutiny.Uni;

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
    public Uni<Void> send() {
        if (templateInstance.getAttribute(VariantTemplate.VARIANTS) != null) {

            Map<Result, Uni<String>> collector = new LinkedHashMap<>();
            @SuppressWarnings("unchecked")
            List<Variant> variants = (List<Variant>) templateInstance.getAttribute(VARIANTS);
            for (Variant variant : variants) {
                if (variant.mediaType.equals(Variant.TEXT_HTML) || variant.mediaType.equals(Variant.TEXT_PLAIN)) {
                    Result res = new Result(variant,
                            templateInstance.setAttribute(SELECTED_VARIANT, variant).data(data).renderAsync());
                    collector.put(res, res.toUni());
                }
            }

            if (collector.isEmpty()) {
                throw new IllegalStateException("No suitable template variant found");
            }

            Function<List<String>, Map<Result, String>> combinator = list -> {
                Map<Result, String> results = new LinkedHashMap<>();
                Iterator<Result> iteratorOverResult = collector.keySet().iterator();
                Iterator<String> iteratorOverRenderedResult = list.iterator();

                while (iteratorOverResult.hasNext()) {
                    Result next = iteratorOverResult.next();
                    results.put(next, iteratorOverRenderedResult.next());
                }
                return results;
            };

            Uni<Map<Result, String>> uni = Uni.combine().all()
                    .unis(collector.values())
                    .combinedWith(combinator);

            return uni
                    .onItem().produceUni(map -> {
                        map.entrySet().forEach(entry -> {
                            if (entry.getKey().variant.mediaType.equals(Variant.TEXT_HTML)) {
                                mail.setHtml(entry.getValue());
                            } else if (entry.getKey().variant.mediaType.equals(Variant.TEXT_PLAIN)) {
                                mail.setText(entry.getValue());
                            }
                        });
                        return mailer.send(mail);
                    });
        } else {
            throw new IllegalStateException("No template variant found");
        }
    }

    static class Result {

        final Variant variant;
        final CompletionStage<String> value;

        public Result(Variant variant, CompletionStage<String> result) {
            this.variant = variant;
            this.value = result;
        }

        Uni<String> toUni() {
            return Uni.createFrom().completionStage(value);
        }
    }

}
