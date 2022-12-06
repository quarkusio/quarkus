package io.quarkus.undertow.runtime;

import java.util.function.Function;

import jakarta.inject.Singleton;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.SecurityInfo;
import io.undertow.servlet.api.SingleConstraintMatch;
import io.undertow.servlet.handlers.security.SecurityPathMatch;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class ServletHttpSecurityPolicy implements HttpSecurityPolicy {

    private volatile Deployment deployment;

    //the context path, guaranteed to have a trailing /
    private volatile String contextPath;

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {

        String requestPath = request.request().path();
        if (!requestPath.startsWith(contextPath)) {
            //anything outside the context path we don't have anything to do with
            return Uni.createFrom().item(CheckResult.PERMIT);
        }
        if (!contextPath.equals("/")) {
            requestPath = requestPath.substring(contextPath.length() - 1);
        }
        SecurityPathMatch match = deployment.getSecurityPathMatches().getSecurityInfo(requestPath,
                request.request().method().name());

        SingleConstraintMatch mergedConstraint = match.getMergedConstraint();
        if (mergedConstraint.getRequiredRoles().isEmpty()) {
            SecurityInfo.EmptyRoleSemantic emptyRoleSemantic = mergedConstraint.getEmptyRoleSemantic();
            if (emptyRoleSemantic == SecurityInfo.EmptyRoleSemantic.PERMIT) {
                return Uni.createFrom().item(CheckResult.PERMIT);
            } else if (emptyRoleSemantic == SecurityInfo.EmptyRoleSemantic.DENY) {
                return Uni.createFrom().item(CheckResult.DENY);
            } else if (emptyRoleSemantic == SecurityInfo.EmptyRoleSemantic.AUTHENTICATE) {
                return identity.map(new Function<SecurityIdentity, CheckResult>() {
                    @Override
                    public CheckResult apply(SecurityIdentity securityIdentity) {
                        if (securityIdentity.isAnonymous()) {
                            return CheckResult.DENY;
                        } else {
                            return CheckResult.PERMIT;
                        }
                    }
                });
            } else {
                return Uni.createFrom().failure(new RuntimeException("Unknown empty role semantic " + emptyRoleSemantic));
            }
        } else {
            return identity.map(new Function<SecurityIdentity, CheckResult>() {
                @Override
                public CheckResult apply(SecurityIdentity securityIdentity) {
                    for (String i : mergedConstraint.getRequiredRoles()) {
                        if (securityIdentity.hasRole(i)) {
                            return CheckResult.PERMIT;
                        }
                    }
                    return CheckResult.DENY;
                }
            });
        }
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public ServletHttpSecurityPolicy setDeployment(Deployment deployment) {
        this.deployment = deployment;
        contextPath = deployment.getDeploymentInfo().getContextPath();
        if (!contextPath.endsWith("/")) {
            contextPath = contextPath + "/";
        }
        return this;
    }
}
