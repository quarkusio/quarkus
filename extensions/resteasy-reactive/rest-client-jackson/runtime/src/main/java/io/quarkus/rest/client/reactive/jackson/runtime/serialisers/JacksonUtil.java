package io.quarkus.rest.client.reactive.jackson.runtime.serialisers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;

final class JacksonUtil {

    static final ConcurrentMap<ResolverMapKey, ObjectMapper> contextResolverMap = new ConcurrentHashMap<>();

    private JacksonUtil() {
    }

    static ObjectMapper getObjectMapperFromContext(MediaType responseMediaType, RestClientRequestContext context) {
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
            var key = new ResolverMapKey(context.getConfiguration(),
                    context.getInvokedMethod() != null ? context.getInvokedMethod().getDeclaringClass() : null);
            return contextResolverMap.computeIfAbsent(key, new Function<>() {
                @Override
                public ObjectMapper apply(ResolverMapKey resolverMapKey) {
                    return cr.getContext(resolverMapKey.getRestClientClass());
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
