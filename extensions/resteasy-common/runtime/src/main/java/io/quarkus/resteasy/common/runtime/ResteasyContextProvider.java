package io.quarkus.resteasy.common.runtime;

import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.jboss.resteasy.core.ResteasyContext;

public class ResteasyContextProvider implements ThreadContextProvider {

    private static final String JAXRS_CONTEXT = "JAX-RS";

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        Map<Class<?>, Object> context = ResteasyContext.getContextDataMap();
        return () -> {
            ResteasyContext.pushContextDataMap(context);
            return () -> {
                ResteasyContext.removeContextDataLevel();
            };
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        Map<Class<?>, Object> context = Collections.emptyMap();
        return () -> {
            ResteasyContext.pushContextDataMap(context);
            return () -> {
                ResteasyContext.removeContextDataLevel();
            };
        };
    }

    @Override
    public String getThreadContextType() {
        return JAXRS_CONTEXT;
    }
}
