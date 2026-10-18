package io.quarkus.grpc.auth;

import java.util.function.BiFunction;

import jakarta.inject.Singleton;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class BlockingHttpSecurityPolicy implements HttpSecurityPolicy {

    final static String BLOCK_REQUEST = "block-request";

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        if (request.request().headers().get(BLOCK_REQUEST) != null) {
            return requestContext.runBlocking(request, identity,
                    new BiFunction<RoutingContext, SecurityIdentity, CheckResult>() {
                        @Override
                        public CheckResult apply(RoutingContext routingContext, SecurityIdentity securityIdentity) {
                            return CheckResult.PERMIT;
                        }
                    });
        } else {
            return CheckResult.permit();
        }
    }
}
