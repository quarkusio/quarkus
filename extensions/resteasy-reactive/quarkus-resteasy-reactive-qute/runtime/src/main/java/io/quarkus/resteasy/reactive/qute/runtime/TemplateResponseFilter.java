package io.quarkus.resteasy.reactive.qute.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.smallrye.mutiny.Uni;

public class TemplateResponseFilter {

    @SuppressWarnings("unchecked")
    @ServerResponseFilter
    public Uni<Void> filter(ResteasyReactiveContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object entity = responseContext.getEntity();
        if (!(entity instanceof TemplateInstance)) {
            return null;
        }

        MediaType mediaType;
        TemplateInstance instance = (TemplateInstance) entity;
        Object variantsAttr = instance.getAttribute(TemplateInstance.VARIANTS);
        if (variantsAttr != null) {
            List<javax.ws.rs.core.Variant> variants = new ArrayList<>();
            for (Variant variant : (List<Variant>) variantsAttr) {
                variants.add(new javax.ws.rs.core.Variant(MediaType.valueOf(variant.getMediaType()), variant.getLocale(),
                        variant.getEncoding()));
            }
            javax.ws.rs.core.Variant selected = requestContext.getRequest()
                    .selectVariant(variants);

            if (selected != null) {
                Locale selectedLocale = selected.getLanguage();
                if (selectedLocale == null) {
                    List<Locale> acceptableLocales = requestContext.getAcceptableLanguages();
                    if (!acceptableLocales.isEmpty()) {
                        selectedLocale = acceptableLocales.get(0);
                    }
                }
                instance.setAttribute(TemplateInstance.SELECTED_VARIANT,
                        new Variant(selectedLocale, selected.getMediaType().toString(), selected.getEncoding()));
                mediaType = selected.getMediaType();
            } else {
                mediaType = responseContext.getMediaType();
            }
        } else {
            mediaType = responseContext.getMediaType();
        }

        return instance.createUni().chain(r -> {
            if (mediaType != null) {
                responseContext.setEntity(r, null, mediaType);
            } else {
                responseContext.setEntity(r);
            }
            return Uni.createFrom().nullItem();
        });
    }
}
