package io.quarkus.spring.web.runtime;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.springframework.http.ResponseEntity;

public class ResponseEntityHandler implements ServerRestHandler {

    @Override

    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        Object result = requestContext.getResult();
        if (result instanceof ResponseEntity) {
            requestContext.setResult(ResponseEntityConverter.toResponse((ResponseEntity<?>) result));
        }
    }
}
