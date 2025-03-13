package io.quarkus.spring.web.resteasy.reactive.runtime;

import java.util.List;
import java.util.Map;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class SpringMultiValueMapParamExtractor implements ParameterExtractor {

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        Map<String, List<String>> parametersMap = context.serverRequest().getQueryParamsMap();
        MultiValueMap<String, String> springMap = new LinkedMultiValueMap<>();
        parametersMap.forEach(springMap::put);
        return springMap;
    }
}
