package io.quarkus.undertow.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Singleton;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
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
    public CompletionStage<CheckResult> checkPermission(RoutingContext request, SecurityIdentity identity,
            AuthorizationRequestContext requestContext) {

        String requestPath = request.request().path();
        if (!requestPath.startsWith(contextPath)) {
            //anything outside the context path we don't have anything to do with
            return CompletableFuture.completedFuture(CheckResult.PERMIT);
        }
        if (!contextPath.equals("/")) {
            requestPath = requestPath.substring(contextPath.length() - 1);
        }
        SecurityPathMatch match = deployment.getSecurityPathMatches().getSecurityInfo(requestPath,
                request.request().rawMethod());

        SingleConstraintMatch mergedConstraint = match.getMergedConstraint();
        if (mergedConstraint.getRequiredRoles().isEmpty()) {
            SecurityInfo.EmptyRoleSemantic emptyRoleSemantic = mergedConstraint.getEmptyRoleSemantic();
            if (emptyRoleSemantic == SecurityInfo.EmptyRoleSemantic.PERMIT) {
                return CompletableFuture.completedFuture(CheckResult.PERMIT);
            } else if (emptyRoleSemantic == SecurityInfo.EmptyRoleSemantic.DENY) {
                return CompletableFuture.completedFuture(CheckResult.DENY);
            } else if (emptyRoleSemantic == SecurityInfo.EmptyRoleSemantic.AUTHENTICATE) {
                if (identity.isAnonymous()) {
                    return CompletableFuture.completedFuture(CheckResult.DENY);
                } else {
                    return CompletableFuture.completedFuture(CheckResult.PERMIT);
                }
            } else {
                CompletableFuture<CheckResult> c = new CompletableFuture<>();
                c.completeExceptionally(new RuntimeException("Unknown empty role semantic " + emptyRoleSemantic));
                return c;
            }
        } else {
            for (String i : mergedConstraint.getRequiredRoles()) {
                if (identity.hasRole(i)) {
                    return CompletableFuture.completedFuture(CheckResult.PERMIT);
                }
            }
            return CompletableFuture.completedFuture(CheckResult.DENY);
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
