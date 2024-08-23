package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.JavaScriptRequestChecker;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomJavaScriptRequestChecker implements JavaScriptRequestChecker {

    @Override
    public boolean isJavaScriptRequest(RoutingContext context) {
        return "true".equals(context.request().getHeader("HX-Request"));
    }

}
