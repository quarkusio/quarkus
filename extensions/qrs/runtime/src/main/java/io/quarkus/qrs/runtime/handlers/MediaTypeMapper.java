package io.quarkus.qrs.runtime.handlers;

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
            MediaType produces = i.getProduces();
            if (produces == null) {
                produces = MediaType.WILDCARD_TYPE;
            }
            resources.get(mt).resources.put(produces, i);
        }
        for (Holder i : resources.values()) {
            i.producesTypes.addAll(i.resources.keySet());
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
            requestContext.setThrowable(new WebApplicationException(Response.status(416).build()));
            return;
        }
        RuntimeResource selectedResource = null;
        if (selected.resources.size() == 1) {
            selectedResource = selected.resources.values().iterator().next();
        } else {
            String accept = requestContext.getContext().request().headers().get(HttpHeaders.ACCEPT);
            List<MediaType> desired = MediaTypeHelper.parseHeader(accept);

            MediaType produces = MediaTypeHelper.getBestMatch(desired, selected.producesTypes);
            selectedResource = selected.resources.get(produces);
            if (selectedResource == null) {
                selectedResource = selected.resources.get(MediaType.WILDCARD_TYPE);
            }
        }

        if (selectedResource == null) {
            requestContext.setThrowable(new WebApplicationException(Response.status(416).build()));
            return;
        }
        requestContext.restart(selectedResource);
    }

    final static class Holder {

        final Map<MediaType, RuntimeResource> resources = new HashMap<>();
        final List<MediaType> producesTypes = new ArrayList<>();
    }
}
