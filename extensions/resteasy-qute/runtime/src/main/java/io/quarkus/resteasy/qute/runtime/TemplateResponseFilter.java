package io.quarkus.resteasy.qute.runtime;

import static io.quarkus.qute.api.VariantTemplate.SELECTED_VARIANT;
import static io.quarkus.qute.api.VariantTemplate.VARIANTS;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.core.interception.jaxrs.SuspendableContainerResponseContext;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.quarkus.qute.api.VariantTemplate;

@Provider
public class TemplateResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        Object entity = responseContext.getEntity();
        if (entity instanceof TemplateInstance) {
            SuspendableContainerResponseContext ctx = (SuspendableContainerResponseContext) responseContext;
            ctx.suspend();

            MediaType mediaType;
            TemplateInstance rendering = (TemplateInstance) entity;

            if (rendering.getAttribute(VariantTemplate.VARIANTS) != null) {
                @SuppressWarnings("unchecked")
                List<Variant> variants = (List<Variant>) rendering.getAttribute(VARIANTS);
                javax.ws.rs.core.Variant selected = requestContext.getRequest()
                        .selectVariant(variants.stream()
                                .map(v -> new javax.ws.rs.core.Variant(MediaType.valueOf(v.mediaType), v.locale, v.encoding))
                                .collect(Collectors.toList()));
                if (selected != null) {
                    rendering.setAttribute(SELECTED_VARIANT,
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
                rendering.renderAsync()
                        .whenComplete((r, t) -> {
                            if (t == null) {
                                Response resp = Response.ok(r, mediaType).build();
                                // make sure we avoid setting a null media type because that causes
                                // an NPE further down
                                if (resp.getMediaType() != null) {
                                    ctx.setEntity(resp.getEntity(), null, resp.getMediaType());
                                } else {
                                    ctx.setEntity(resp.getEntity());
                                }
                                ctx.setStatus(resp.getStatus());
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
