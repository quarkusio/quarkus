package io.quarkus.resteasy.qute.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.core.interception.jaxrs.SuspendableContainerResponseContext;

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

            try {
                instance.renderAsync()
                        .whenComplete((r, t) -> {
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
