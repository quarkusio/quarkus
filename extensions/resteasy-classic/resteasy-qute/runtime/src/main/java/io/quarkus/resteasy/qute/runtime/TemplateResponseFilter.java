package io.quarkus.resteasy.qute.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.core.interception.jaxrs.SuspendableContainerResponseContext;

import io.quarkus.arc.Arc;
import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;

@Provider
public class TemplateResponseFilter implements ContainerResponseFilter {

    @SuppressWarnings("unchecked")
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        Object entity = responseContext.getEntity();
        if (entity instanceof TemplateInstance) {
            SuspendableContainerResponseContext ctx = (SuspendableContainerResponseContext) responseContext;
            ctx.suspend();

            MediaType mediaType;
            TemplateInstance instance = (TemplateInstance) entity;
            Object variantsAttr = instance.getAttribute(TemplateInstance.VARIANTS);
            if (variantsAttr != null) {
                List<jakarta.ws.rs.core.Variant> variants = new ArrayList<>();
                for (Variant variant : (List<Variant>) variantsAttr) {
                    variants.add(new jakarta.ws.rs.core.Variant(MediaType.valueOf(variant.getMediaType()), variant.getLocale(),
                            variant.getEncoding()));
                }
                jakarta.ws.rs.core.Variant selected = requestContext.getRequest()
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

            CompletionStage<String> cs = instance.renderAsync();
            if (!Arc.container().instance(Engine.class).get().useAsyncTimeout()) {
                // Make sure the timeout is always used
                long timeout = instance.getTimeout();
                cs = cs.toCompletableFuture().orTimeout(timeout, TimeUnit.MILLISECONDS);
            }
            try {
                cs.whenComplete((r, t) -> {
                    if (t == null) {
                        // make sure we avoid setting a null media type because that causes
                        // an NPE further down
                        if (mediaType != null) {
                            ctx.setEntity(r, null, mediaType);
                        } else {
                            ctx.setEntity(r);
                        }
                        ctx.resume();
                    } else {
                        ctx.resume(t);
                    }
                });
            } catch (Throwable t) {
                ctx.resume(t);
            }
        }
    }
}
