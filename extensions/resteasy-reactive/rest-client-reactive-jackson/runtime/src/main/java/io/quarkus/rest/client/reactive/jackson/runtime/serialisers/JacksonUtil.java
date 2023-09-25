package io.quarkus.rest.client.reactive.jackson.runtime.serialisers;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;

final class JacksonUtil {

    private JacksonUtil() {
    }

    static ObjectMapper getObjectMapperFromContext(Class<?> type, MediaType responseMediaType, RestClientRequestContext context,
            ConcurrentMap<ResolverMapKey, ObjectMapper> contextResolverMap) {
        Providers providers = getProviders(context);
        if (providers == null) {
            return null;
        }

        ContextResolver<ObjectMapper> contextResolver = providers.getContextResolver(ObjectMapper.class,
                responseMediaType);
        if (contextResolver == null) {
            // TODO: not sure if this is correct, but Jackson does this as well...
            contextResolver = providers.getContextResolver(ObjectMapper.class, null);
        }
        if (contextResolver != null) {
            var cr = contextResolver;
            var key = new ResolverMapKey(type, context.getConfiguration(), context.getInvokedMethod().getDeclaringClass());
            return contextResolverMap.computeIfAbsent(key, new Function<>() {
                @Override
                public ObjectMapper apply(ResolverMapKey resolverMapKey) {
                    return cr.getContext(resolverMapKey.getType());
                }
            });
        }

        return null;
    }

    private static Providers getProviders(RestClientRequestContext context) {
        if (context != null && context.getClientRequestContext() != null) {
            return context.getClientRequestContext().getProviders();
        }

        return null;
    }
}
