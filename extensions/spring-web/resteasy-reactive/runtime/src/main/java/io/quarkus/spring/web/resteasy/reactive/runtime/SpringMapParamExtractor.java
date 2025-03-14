package io.quarkus.spring.web.resteasy.reactive.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class SpringMapParamExtractor implements ParameterExtractor {

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        Map<String, List<String>> parametersMap = context.serverRequest().getQueryParamsMap();
        if (parametersMap != null && !parametersMap.isEmpty()) {
            return parametersMap;
        }
        return Collections.emptyMap();
    }

}
