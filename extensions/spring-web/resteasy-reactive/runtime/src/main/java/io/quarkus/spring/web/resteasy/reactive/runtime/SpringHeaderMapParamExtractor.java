package io.quarkus.spring.web.resteasy.reactive.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

public class SpringHeaderMapParamExtractor implements ParameterExtractor {

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        Map<String, String> headerMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : context.serverRequest().getAllRequestHeaders()) {
            headerMap.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return headerMap;
    }

}