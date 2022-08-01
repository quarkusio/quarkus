package org.jboss.resteasy.reactive.server.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import org.jboss.resteasy.reactive.common.model.ResourceContextResolver;
import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
import org.jboss.resteasy.reactive.server.jaxrs.ContextResolverDelegate;
import org.jboss.resteasy.reactive.spi.BeanFactory;

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
            MediaType bestMatch = null;
            for (ResourceContextResolver goodResolver : goodResolvers) {
                boolean add = false;
                // we don't care
                if (mediaType == null) {
                    add = true;
                } else {
                    MediaType match;
                    // wildcard handling
                    if (goodResolver.mediaTypes().isEmpty()) {
                        match = MediaType.WILDCARD_TYPE;
                    } else {
                        match = MediaTypeHelper.getBestMatch(mt, goodResolver.mediaTypes());
                        // if there's no match, we must skip it
                        if (match == null)
                            continue;
                    }
                    if (bestMatch == null) {
                        bestMatch = match;
                        add = true;
                    } else {
                        int cmp = MediaTypeHelper.Q_COMPARATOR.compare(bestMatch, match);
                        if (cmp == 0) {
                            // same fitness
                            add = true;
                        } else if (cmp > 0) {
                            // wrong order means that our best match is not as good as the new match
                            delegates.clear();
                            add = true;
                            bestMatch = match;
                        }
                        // otherwise this is not as good as our delegate list, so let's not add it
                    }
                }
                if (add) {
                    delegates.add((ContextResolver<T>) goodResolver.getFactory().createInstance().getInstance());
                }
            }
            if (delegates.isEmpty()) {
                return null;
            } else if (delegates.size() == 1) {
                return delegates.get(0);
            }

            return new ContextResolverDelegate<>(delegates);
        }
        return null;
    }

    public Map<Class<?>, List<ResourceContextResolver>> getResolvers() {
        return resolvers;
    }

    public void initializeDefaultFactories(Function<String, BeanFactory<?>> factoryCreator) {
        for (Map.Entry<Class<?>, List<ResourceContextResolver>> entry : resolvers.entrySet()) {
            for (ResourceContextResolver i : entry.getValue()) {
                if (i.getFactory() == null) {
                    i.setFactory((BeanFactory) factoryCreator.apply(i.getClassName()));
                }
            }
        }
    }
}
