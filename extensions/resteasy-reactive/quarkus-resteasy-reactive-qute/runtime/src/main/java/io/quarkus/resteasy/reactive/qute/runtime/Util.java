package io.quarkus.resteasy.reactive.qute.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.smallrye.mutiny.Uni;

final class Util {

    private Util() {
    }

    static Uni<String> toUni(TemplateInstance instance, Engine engine) {
        Uni<String> uni = instance.createUni();
        if (!engine.useAsyncTimeout()) {
            // Make sure the timeout is always used
            long timeout = instance.getTimeout();
            uni = uni.ifNoItem().after(Duration.ofMillis(timeout))
                    .failWith(() -> new TemplateException(instance + " rendering timeout [" + timeout + "ms] occurred"));
        }
        return uni;
    }

    @SuppressWarnings("unchecked")
    static MediaType setSelectedVariant(TemplateInstance result,
            Request request, List<Locale> acceptableLanguages) {
        Object variantsAttr = result.getAttribute(TemplateInstance.VARIANTS);
        if (variantsAttr != null) {
            List<Variant> quteVariants = (List<Variant>) variantsAttr;
            List<jakarta.ws.rs.core.Variant> jaxRsVariants = new ArrayList<>(quteVariants.size());
            for (Variant variant : quteVariants) {
                jaxRsVariants.add(new jakarta.ws.rs.core.Variant(MediaType.valueOf(variant.getMediaType()), variant.getLocale(),
                        variant.getEncoding()));
            }
            jakarta.ws.rs.core.Variant selected = request
                    .selectVariant(jaxRsVariants);

            if (selected != null) {
                Locale selectedLocale = selected.getLanguage();
                if (selectedLocale == null) {
                    if (!acceptableLanguages.isEmpty()) {
                        selectedLocale = acceptableLanguages.get(0);
                    }
                }
                result.setAttribute(TemplateInstance.SELECTED_VARIANT,
                        new Variant(selectedLocale, selected.getMediaType().toString(),
                                selected.getEncoding()));
                return selected.getMediaType();
            }
        }
        return null;
    }
}
