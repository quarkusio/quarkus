package io.quarkus.resteasy.reactive.qute.runtime;

import static io.quarkus.resteasy.reactive.qute.runtime.Util.setSelectedVariant;
import static io.quarkus.resteasy.reactive.qute.runtime.Util.toUni;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;

/**
 * This class is needed in order to support handling {@link jakarta.ws.rs.core.Response} that contains a TemplateInstance...
 */
public class TemplateResponseFilter {

    @Inject
    Engine engine;

    @ServerResponseFilter
    public Uni<Void> filter(ResteasyReactiveContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object entity = responseContext.getEntity();
        if (!(entity instanceof TemplateInstance)) {
            return null;
        }

        MediaType mediaType;
        TemplateInstance instance = (TemplateInstance) entity;
        MediaType selectedMediaType = setSelectedVariant(instance, requestContext.getRequest(),
                HeaderUtil.getAcceptableLanguages(requestContext.getHeaders()));
        if (selectedMediaType == null) {
            mediaType = responseContext.getMediaType();
        } else {
            mediaType = selectedMediaType;
        }

        Uni<String> uni = toUni(instance, engine);
        return uni.chain(r -> {
            if (mediaType != null) {
                responseContext.setEntity(r, null, mediaType);
            } else {
                responseContext.setEntity(r);
            }
            return Uni.createFrom().nullItem();
        });
    }

}
