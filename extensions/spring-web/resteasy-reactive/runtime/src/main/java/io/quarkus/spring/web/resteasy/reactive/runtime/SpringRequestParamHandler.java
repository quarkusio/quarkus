package io.quarkus.spring.web.resteasy.reactive.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * In Spring, parameters annotated with {@code @RequestParam} are required by default unless explicitly marked as
 * optional.
 * This {@link SpringRequestParamHandler} enforces the required constraint responding with a BAD_REQUEST status.
 *
 */
public class SpringRequestParamHandler implements HandlerChainCustomizer {
    @Override
    public List<ServerRestHandler> handlers(HandlerChainCustomizer.Phase phase, ResourceClass resourceClass,
            ServerResourceMethod resourceMethod) {
        if (phase == Phase.AFTER_RESPONSE_CREATED) {
            return Collections.singletonList(new ServerRestHandler() {
                @Override
                public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
                    Map<String, List<String>> parametersMap = requestContext.serverRequest().getQueryParamsMap();
                    if (parametersMap.isEmpty()) {
                        ResponseImpl response = (ResponseImpl) requestContext.getResponse().get();
                        response.setStatus(400);
                    }
                }
            });
        }
        return Collections.emptyList();
    }
}
