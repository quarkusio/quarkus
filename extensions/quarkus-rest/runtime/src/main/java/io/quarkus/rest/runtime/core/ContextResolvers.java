package io.quarkus.rest.runtime.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;

import io.quarkus.rest.runtime.jaxrs.QuarkusRestContextResolverDelegate;
import io.quarkus.rest.runtime.model.ResourceContextResolver;
import io.quarkus.rest.runtime.util.MediaTypeHelper;

public class ContextResolvers {

    private final Map<Class<?>, List<ResourceContextResolver>> resolvers = new HashMap<>();

    public <T> void addContextResolver(Class<T> contextType, ResourceContextResolver contextResolver) {
        List<ResourceContextResolver> list = resolvers.get(contextType);
        if (list == null) {
            list = new ArrayList<>(1);
            resolvers.put(contextType, list);
        }
        list.add(contextResolver);
    }

    public <T> ContextResolver<T> getContextResolver(Class<T> clazz, MediaType mediaType) {
        List<ResourceContextResolver> goodResolvers = resolvers.get(clazz);
        if ((goodResolvers != null) && !goodResolvers.isEmpty()) {
            List<MediaType> mt = Collections.singletonList(mediaType);
            final List<ContextResolver<T>> delegates = new ArrayList<>();
            for (ResourceContextResolver goodResolver : goodResolvers) {
                MediaType match = MediaTypeHelper.getBestMatch(mt, goodResolver.mediaTypes());
                if (match != null || mediaType == null) {
                    delegates.add((ContextResolver<T>) goodResolver.getFactory().createInstance().getInstance());
                }
            }
            if (delegates.isEmpty()) {
                return null;
            } else if (delegates.size() == 1) {
                return delegates.get(0);
            }

            return new QuarkusRestContextResolverDelegate<>(delegates);
        }
        return null;
    }
}
