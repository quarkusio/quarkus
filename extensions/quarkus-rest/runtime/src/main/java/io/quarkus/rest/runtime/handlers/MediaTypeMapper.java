package io.quarkus.rest.runtime.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.mapping.RuntimeResource;
import io.quarkus.rest.runtime.util.MediaTypeHelper;

/**
 * Handler that deals with the case when two methods have the same path,
 * and it needs to select based on content type.
 * 
 * This is not super optimised, as it is not a common case. Most apps
 * won't every use this handler.
 */
public class MediaTypeMapper implements RestHandler {

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

    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        String contentType = requestContext.getContext().request().headers().get(HttpHeaders.CONTENT_TYPE);
        Holder selectedHolder = null;
        if (contentType == null) {
            selectedHolder = resourcesByConsumes.get(MediaType.WILDCARD_TYPE);
        } else {
            MediaType consumes = MediaTypeHelper.getBestMatch(consumesTypes,
                    Collections.singletonList(MediaType.valueOf(contentType)));
            selectedHolder = resourcesByConsumes.get(consumes);
            if (selectedHolder == null) {
                selectedHolder = resourcesByConsumes.get(MediaType.WILDCARD_TYPE);
            }
        }
        if (selectedHolder == null) {
            throw new WebApplicationException(Response.status(416).build());
        }
        RuntimeResource selectedResource;
        if (selectedHolder.mtWithoutParamsToResource.size() == 1) {
            selectedResource = selectedHolder.mtWithoutParamsToResource.values().iterator().next();
        } else {
            MediaType produces = selectMediaType(selectedHolder.mtsWithParams,
                    requestContext.getContext().request().getHeader(HttpHeaderNames.ACCEPT));
            requestContext.setProducesMediaType(produces);
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

    // TODO: this is probably too naive but it works better with the TCK than ServerMediaType#negotiateProduces
    public MediaType selectMediaType(List<MediaType> availableMediaTypes, String accept) {
        MediaType selected = null;
        if (accept != null) {
            List<MediaType> acceptedMediaTypes = MediaTypeHelper.parseHeader(accept);
            selected = MediaTypeHelper.getBestConcreteMatch(acceptedMediaTypes, availableMediaTypes);
        }
        if (selected == null) {
            selected = availableMediaTypes.get(0);
        }
        if (selected.equals(MediaType.WILDCARD_TYPE)) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
        return selected;
    }

    private static final class Holder {

        private final Map<MediaType, RuntimeResource> mtWithoutParamsToResource = new HashMap<>();
        private final List<MediaType> mtsWithParams = new ArrayList<>();

        public void setResource(RuntimeResource runtimeResource, MediaType mediaType) {
            MediaType withoutParams = mediaType;
            MediaType withParas = mediaType;
            if (!mediaType.getParameters().isEmpty()) {
                withoutParams = new MediaType(mediaType.getType(), mediaType.getSubtype());
            }
            mtWithoutParamsToResource.put(withoutParams, runtimeResource);
            mtsWithParams.add(withParas);
        }
    }
}
