package org.jboss.resteasy.reactive.server.handlers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * Handler that deals with the case when two methods have the same path,
 * and it needs to select based on content type.
 * <p>
 * This is not super optimised, as it is not a common case. Most apps
 * won't every use this handler.
 */
public class MediaTypeMapper implements ServerRestHandler {

    private static final MediaType[] DEFAULT_MEDIA_TYPES = new MediaType[] { MediaType.WILDCARD_TYPE };
    private static final List<MediaType> DEFAULT_MEDIA_TYPES_LIST = List.of(DEFAULT_MEDIA_TYPES);

    final Map<MediaType, Holder> resourcesByConsumes;
    final List<MediaType> consumesTypes;

    public MediaTypeMapper(List<RuntimeResource> runtimeResources) {
        resourcesByConsumes = new HashMap<>();
        consumesTypes = new ArrayList<>();
        for (RuntimeResource runtimeResource : runtimeResources) {
            List<MediaType> consumesMediaTypes = getConsumesMediaTypes(runtimeResource);
            for (MediaType consumedMediaType : consumesMediaTypes) {
                if (!resourcesByConsumes.containsKey(consumedMediaType)) {
                    consumesTypes.add(consumedMediaType);
                    resourcesByConsumes.put(consumedMediaType, new Holder());
                }
            }
            for (MediaType producesMT : getProducesMediaTypes(runtimeResource)) {
                for (MediaType consumedMediaType : consumesMediaTypes) {
                    resourcesByConsumes.get(consumedMediaType).setResource(runtimeResource, producesMT);
                }
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
            throw new NotSupportedException("The content-type header value did not match the value in @Consumes");
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
        List<String> accepts = requestContext.getHttpHeaders().getRequestHeader(HttpHeaders.ACCEPT);
        for (String accept : accepts) {
            Map.Entry<MediaType, MediaType> entry = holder.serverMediaType
                    .negotiateProduces(accept, null);
            if (entry.getValue() != null) {
                selected = entry.getValue();
                break;
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

    private MediaType[] getProducesMediaTypes(RuntimeResource runtimeResource) {
        return runtimeResource.getProduces() == null
                ? DEFAULT_MEDIA_TYPES
                : runtimeResource.getProduces().getSortedOriginalMediaTypes();
    }

    private List<MediaType> getConsumesMediaTypes(RuntimeResource runtimeResource) {
        return runtimeResource.getConsumes().isEmpty() ? DEFAULT_MEDIA_TYPES_LIST
                : runtimeResource.getConsumes();
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
            // TODO: this isn't completely correct as we are supposed to take q and then qs into account...
            MediaTypeHelper.sortByQSWeight(mtsWithParams);
            serverMediaType = new ServerMediaType(mtsWithParams, StandardCharsets.UTF_8.name(), true);
        }
    }
}
