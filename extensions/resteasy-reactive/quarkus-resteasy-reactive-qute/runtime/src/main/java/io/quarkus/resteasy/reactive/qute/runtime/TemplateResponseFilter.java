package io.quarkus.resteasy.reactive.qute.runtime;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;

public class TemplateResponseFilter {

    @ServerResponseFilter
    public void filter(ResteasyReactiveContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object entity = responseContext.getEntity();
        if (!(entity instanceof TemplateInstance)) {
            return;
        }

        requestContext.suspend();
        MediaType mediaType;
        TemplateInstance instance = (TemplateInstance) entity;
        Object variantsAttr = instance.getAttribute(TemplateInstance.VARIANTS);
        if (variantsAttr != null) {
            @SuppressWarnings("unchecked")
            List<Variant> variants = (List<Variant>) variantsAttr;
            javax.ws.rs.core.Variant selected = requestContext.getRequest()
                    .selectVariant(variants.stream()
                            .map(v -> new javax.ws.rs.core.Variant(MediaType.valueOf(v.getMediaType()), v.getLocale(),
                                    v.getEncoding()))
                            .collect(Collectors.toList()));
            if (selected != null) {
                instance.setAttribute(TemplateInstance.SELECTED_VARIANT,
                        new Variant(selected.getLanguage(), selected.getMediaType().toString(), selected.getEncoding()));
                mediaType = selected.getMediaType();
            } else {
                // TODO we should use the default
                mediaType = null;
            }
        } else {
            // TODO how to get media type from non-variant templates?
            mediaType = null;
        }

        try {
            instance.renderAsync()
                    .whenComplete((r, t) -> {
                        if (t == null) {
                            // make sure we avoid setting a null media type because that causes
                            // an NPE further down
                            if (mediaType != null) {
                                responseContext.setEntity(r, null, mediaType);
                            } else {
                                responseContext.setEntity(r);
                            }
                            requestContext.resume();
                        } else {
                            requestContext.resume(t);
                        }
                    });
        } catch (Throwable t) {
            requestContext.resume(t);
        }
    }
}
