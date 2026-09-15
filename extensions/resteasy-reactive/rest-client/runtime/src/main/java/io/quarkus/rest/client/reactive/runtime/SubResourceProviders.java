package io.quarkus.rest.client.reactive.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.WebTarget;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

/**
 * Registers the providers declared with {@code @RegisterProvider} on a sub-resource interface on the web target
 * the sub-resource client is built from. Used by generated code.
 */
public final class SubResourceProviders {

    private SubResourceProviders() {
    }

    /**
     * Returns a copy of {@code target} with its own configuration on which the given providers are registered, so that
     * they apply to the sub-resource client only. Providers are registered the same way {@link RestClientBuilderImpl}
     * registers the providers declared on the root interface: the CDI bean is used when there is one, and a
     * {@link ResponseExceptionMapper} is added, according to its priority, to the mappers that already apply to the
     * target.
     */
    public static WebTarget register(WebTarget target, Class<?>[] providerClasses, int[] priorities) {
        WebTargetImpl result = ((WebTargetImpl) target).withIsolatedConfiguration();
        List<ResponseExceptionMapper<?>> exceptionMappers = new ArrayList<>();
        for (int i = 0; i < providerClasses.length; i++) {
            Class<?> providerClass = providerClasses[i];
            Object provider = providerClass;
            InstanceHandle<?> instance = Arc.container().instance(providerClass);
            if (instance.isAvailable()) {
                provider = instance.get();
            }
            if (ResponseExceptionMapper.class.isAssignableFrom(providerClass)) {
                if (provider instanceof ResponseExceptionMapper) {
                    exceptionMappers.add((ResponseExceptionMapper<?>) provider);
                } else {
                    exceptionMappers.add((ResponseExceptionMapper<?>) instantiate(providerClass));
                }
            }
            result = result.register(provider, priorities[i]);
        }
        if (!exceptionMappers.isEmpty()) {
            exceptionMappers.addAll(inheritedExceptionMappers(result.getConfiguration()));
            exceptionMappers.sort(Comparator.comparingInt(ResponseExceptionMapper::getPriority));
            result = result.property(MicroProfileRestClientResponseFilter.EXCEPTION_MAPPERS_PROPERTY,
                    Collections.unmodifiableList(exceptionMappers));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<ResponseExceptionMapper<?>> inheritedExceptionMappers(ConfigurationImpl configuration) {
        Object inherited = configuration.getProperty(MicroProfileRestClientResponseFilter.EXCEPTION_MAPPERS_PROPERTY);
        if (inherited instanceof List) {
            return (List<ResponseExceptionMapper<?>>) inherited;
        }
        for (ClientResponseFilter filter : configuration.getResponseFilters()) {
            if (filter instanceof MicroProfileRestClientResponseFilter) {
                return ((MicroProfileRestClientResponseFilter) filter).getExceptionMappers();
            }
        }
        return Collections.emptyList();
    }

    private static Object instantiate(Class<?> providerClass) {
        try {
            return providerClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Failed to instantiate provider " + providerClass
                    + ". Does it have a public no-arg constructor?", e);
        }
    }
}
