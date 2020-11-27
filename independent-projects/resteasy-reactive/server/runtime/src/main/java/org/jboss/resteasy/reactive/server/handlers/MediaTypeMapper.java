package org.jboss.resteasy.reactive.server.handlers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * Handler that deals with the case when two methods have the same path,
 * and it needs to select based on content type.
 * <p>
 * This is not super optimised, as it is not a common case. Most apps
 * won't every use this handler.
 */
public class MediaTypeMapper implements ServerRestHandler {

    final Map<MediaType, Holder> resourcesByConsumes;
    final List<MediaType> consumesTypes;

    public MediaTypeMapper(List<RuntimeResource> runtimeResources) {
        resourcesByConsumes = new HashMap<>();
        consumesTypes = new ArrayList<>();
        for (RuntimeResource runtimeResource : runtimeResources) {
            MediaType consumesMT = runtimeResource.getConsumes().isEmpty() ? MediaType.WILDCARD_TYPE
                    : runtimeResource.getConsumes().get(0);
            if (!resourcesByConsumes.containsKey(consumesMT)) {
                consumesTypes.add(consumesMT);
                resourcesByConsumes.put(consumesMT, new Holder());
            }
            MediaType[] produces = runtimeResource.getProduces() != null
                    ? runtimeResource.getProduces().getSortedOriginalMediaTypes()
                    : null;
            if (produces == null) {
                produces = new MediaType[] { MediaType.WILDCARD_TYPE };
            }
            for (MediaType producesMT : produces) {
                resourcesByConsumes.get(consumesMT).setResource(runtimeResource, producesMT);
            }
        }
        for (Holder holder : resourcesByConsumes.values()) {
            holder.setupServerMediaType();
        }
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        String contentType = requestContext.serverRequest().getRequestHeader(HttpHeaders.CONTENT_TYPE);
        // if there's no Content-Type it's */*
        MediaType contentMediaType = contentType != null ? MediaType.valueOf(contentType) : MediaType.WILDCARD_TYPE;
        // find the best matching consumes type. Note that the arguments are reversed from their definition
        // of desired/provided, but we do want the result to be a media type we consume, since that's how we key
        // our methods, rather than the single media type we get from the client. This way we ensure we get the
        // best match.
        MediaType consumes = MediaTypeHelper.getBestMatch(Collections.singletonList(contentMediaType),
                consumesTypes);
        Holder selectedHolder = resourcesByConsumes.get(consumes);
        // if we haven't found anything, try selecting the wildcard type, if any
        if (selectedHolder == null) {
            selectedHolder = resourcesByConsumes.get(MediaType.WILDCARD_TYPE);
        }
        if (selectedHolder == null) {
            throw new WebApplicationException(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
        }
        RuntimeResource selectedResource;
        if (selectedHolder.mtWithoutParamsToResource.size() == 1) {
            selectedResource = selectedHolder.mtWithoutParamsToResource.values().iterator().next();
        } else {
            MediaType produces = selectMediaType(requestContext, selectedHolder);
            requestContext.setResponseContentType(produces);
            MediaType key = produces;
            if (!key.getParameters().isEmpty()) {
                key = new MediaType(key.getType(), key.getSubtype());
            }
            selectedResource = selectedHolder.mtWithoutParamsToResource.get(key);
            if (selectedResource == null) {
                selectedResource = selectedHolder.mtWithoutParamsToResource.get(MediaType.WILDCARD_TYPE);
            }
        }

        if (selectedResource == null) {
            throw new WebApplicationException(Response.status(416).build());
        }
        requestContext.restart(selectedResource);
    }

    public MediaType selectMediaType(ResteasyReactiveRequestContext requestContext, Holder holder) {
        MediaType selected = null;
        ServerHttpRequest httpServerRequest = requestContext.serverRequest();
        if (httpServerRequest.containsRequestHeader(HttpHeaders.ACCEPT)) {
            Map.Entry<MediaType, MediaType> entry = holder.serverMediaType
                    .negotiateProduces(requestContext.serverRequest().getRequestHeader(HttpHeaders.ACCEPT), null);
            if (entry.getValue() != null) {
                selected = entry.getValue();
            }
        }
        if (selected == null) {
            selected = holder.mtsWithParams.get(0);
        }
        if (selected.equals(MediaType.WILDCARD_TYPE)) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
        return selected;
    }

    private static final class Holder {

        private final Map<MediaType, RuntimeResource> mtWithoutParamsToResource = new HashMap<>();
        private final List<MediaType> mtsWithParams = new ArrayList<>();
        private ServerMediaType serverMediaType;

        public void setResource(RuntimeResource runtimeResource, MediaType mediaType) {
            MediaType withoutParams = mediaType;
            MediaType withParas = mediaType;
            if (!mediaType.getParameters().isEmpty()) {
                withoutParams = new MediaType(mediaType.getType(), mediaType.getSubtype());
            }
            mtWithoutParamsToResource.put(withoutParams, runtimeResource);
            mtsWithParams.add(withParas);
        }

        public void setupServerMediaType() {
            serverMediaType = new ServerMediaType(mtsWithParams, StandardCharsets.UTF_8.name(), true);
        }
    }
}
