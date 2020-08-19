package io.quarkus.qrs.runtime.handlers;

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

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.mapping.RuntimeResource;
import io.quarkus.qrs.runtime.util.MediaTypeHelper;
import io.quarkus.qrs.runtime.util.ServerMediaType;

/**
 * Handler that deals with the case when two methods have the same path,
 * and it needs to select based on content type.
 * 
 * This is not super optimised, as it is not a common case. Most apps
 * won't every use this handler.
 */
public class MediaTypeMapper implements RestHandler {

    final Map<MediaType, Holder> resources;
    final List<MediaType> consumesTypes;

    public MediaTypeMapper(List<RuntimeResource> runtimeResources) {
        resources = new HashMap<>();
        consumesTypes = new ArrayList<>();
        for (RuntimeResource i : runtimeResources) {
            MediaType mt = i.getConsumes();
            if (mt == null) {
                mt = MediaType.WILDCARD_TYPE;
            }
            if (!resources.containsKey(mt)) {
                consumesTypes.add(mt);
                resources.put(mt, new Holder());
            }
            MediaType[] produces = i.getProduces().getSortedOriginalMediaTypes();
            if (produces == null) {
                produces = new MediaType[] { MediaType.WILDCARD_TYPE };
            }
            for (MediaType j : produces) {
                resources.get(mt).resources.put(new MediaType(j.getType(), j.getSubtype()), i);
            }
        }
        for (Holder i : resources.values()) {
            i.producesTypes = new ServerMediaType(new ArrayList<>(i.resources.keySet()), StandardCharsets.UTF_8.name());
        }

    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        String contentType = requestContext.getContext().request().headers().get(HttpHeaders.CONTENT_TYPE);
        Holder selected = null;
        if (contentType == null) {
            selected = resources.get(MediaType.WILDCARD_TYPE);
        } else {
            MediaType consumes = MediaTypeHelper.getBestMatch(consumesTypes,
                    Collections.singletonList(MediaType.valueOf(contentType)));
            selected = resources.get(consumes);
            if (selected == null) {
                selected = resources.get(MediaType.WILDCARD_TYPE);
            }
        }
        if (selected == null) {
            throw new WebApplicationException(Response.status(416).build());
        }
        RuntimeResource selectedResource = null;
        if (selected.resources.size() == 1) {
            selectedResource = selected.resources.values().iterator().next();
        } else {
            MediaType produces = selected.producesTypes.negotiateProduces(requestContext.getContext().request());
            requestContext.setProducesMediaType(produces);
            MediaType key = produces;
            if (!key.getParameters().isEmpty()) {
                key = new MediaType(key.getType(), key.getSubtype());
            }
            selectedResource = selected.resources.get(key);
            if (selectedResource == null) {
                selectedResource = selected.resources.get(MediaType.WILDCARD_TYPE);
            }
        }

        if (selectedResource == null) {
            throw new WebApplicationException(Response.status(416).build());
        }
        requestContext.restart(selectedResource);
    }

    final static class Holder {

        final Map<MediaType, RuntimeResource> resources = new HashMap<>();
        ServerMediaType producesTypes;
    }
}
