package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.JavaScriptRequestChecker;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomJavaScriptRequestChecker implements JavaScriptRequestChecker {

    @Override
    public boolean isJavaScriptRequest(RoutingContext context) {
        return "true".equals(context.request().getHeader("HX-Request"))
                || "true".equals(context.request().getHeader("HX-Problem-Request"));
    }

    @Override
    public ChallengeData getChallenge(RoutingContext context) {
        if ("true".equals(context.request().getHeader("HX-Problem-Request"))) {
            return new ChallengeData(401, "WWW-Authenticate", "OIDC-SPA");
        }
        return null;
    }

}
