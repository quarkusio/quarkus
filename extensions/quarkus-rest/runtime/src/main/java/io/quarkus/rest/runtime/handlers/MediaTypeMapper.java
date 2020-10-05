package io.quarkus.rest.runtime.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.mapping.RuntimeResource;
import io.quarkus.rest.runtime.util.MediaTypeHelper;
import io.vertx.core.http.HttpServerRequest;

/**
 * Handler that deals with the case when two methods have the same path,
 * and it needs to select based on content type.
 * <p>
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

    public MediaType selectMediaType(QuarkusRestRequestContext requestContext, Holder holder) {
        MediaType selected = null;
        HttpServerRequest httpServerRequest = requestContext.getContext().request();
        if (httpServerRequest.headers().contains(HttpHeaderNames.ACCEPT)) {
            List<MediaType> acceptedMediaTypes = requestContext.getHttpHeaders().getModifiableAcceptableMediaTypes();
            if (!acceptedMediaTypes.isEmpty()) {
                MediaTypeHelper.sortByWeight(acceptedMediaTypes);

                List<MediaType> methodMediaTypes = holder.mtsWithParams;
                methodMediaTypes.sort(MethodMediaTypeComparator.INSTANCE);

                selected = doSelectMediaType(methodMediaTypes, acceptedMediaTypes);
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

    // similar to what ServerMediaType#negotiate does but adapted for this use case
    private MediaType doSelectMediaType(List<MediaType> methodMediaTypes, List<MediaType> acceptedMediaTypes) {
        MediaType selected = null;
        String currentClientQ = null;
        int currentServerIndex = Integer.MAX_VALUE;
        for (MediaType desired : acceptedMediaTypes) {
            if (selected != null) {
                //this is to enable server side q values to take effect
                //the client side is sorted by q, if we have already picked one and the q is
                //different then we can return the current one
                if (!Objects.equals(desired.getParameters().get("q"), currentClientQ)) {
                    if (selected.equals(MediaType.WILDCARD_TYPE)) {
                        return MediaType.APPLICATION_OCTET_STREAM_TYPE;
                    }
                    return selected;
                }
            }
            for (int j = 0; j < methodMediaTypes.size(); j++) {
                MediaType provided = methodMediaTypes.get(j);
                if (provided.isCompatible(desired)) {
                    if (selected == null || j < currentServerIndex) {
                        if (provided.isWildcardType()) {
                            selected = MediaType.APPLICATION_OCTET_STREAM_TYPE;
                        } else {
                            selected = provided;
                        }
                        currentServerIndex = j;
                        currentClientQ = desired.getParameters().get("q");
                    }
                }
            }
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

    private static class MethodMediaTypeComparator implements Comparator<MediaType> {

        private final static MethodMediaTypeComparator INSTANCE = new MethodMediaTypeComparator();

        /**
         * The idea here is to de-prioritize wildcards as the spec mentions that they should be picked with lower priority
         * Then we utilize the qs property just like ServerMediaType does
         */
        @Override
        public int compare(MediaType m1, MediaType m2) {
            if (m1.isWildcardType() && !m2.isWildcardType()) {
                return 1;
            }
            if (!m1.isWildcardType() && m2.isWildcardType()) {
                return -1;
            }
            if (!m1.isWildcardType() && !m2.isWildcardType()) {
                if (m1.isWildcardSubtype() && !m2.isWildcardSubtype()) {
                    return 1;
                }
                if (!m1.isWildcardSubtype() && m2.isWildcardSubtype()) {
                    return -1;
                }
            }

            String qs1s = m1.getParameters().get("qs");
            String qs2s = m2.getParameters().get("qs");
            if (qs1s == null && qs2s == null) {
                return 0;
            }
            if (qs1s != null) {
                if (qs2s == null) {
                    return 1;
                } else {
                    float q1 = Float.parseFloat(qs1s);
                    float q2 = Float.parseFloat(qs2s);
                    return Float.compare(q2, q1);
                }
            } else {
                return -1;
            }
        }
    }
}
