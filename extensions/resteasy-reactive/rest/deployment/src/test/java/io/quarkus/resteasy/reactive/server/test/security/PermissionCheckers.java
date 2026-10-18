package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@RequestScoped
public class PermissionCheckers {

    @Inject
    RoutingContext routingContext;

    @Blocking
    @PermissionChecker("worker-thread")
    boolean canRead_WorkerThread(SecurityIdentity identity) {
        if (failIfRequiredByHeader()) {
            return false;
        }
        boolean isAdmin = identity.hasRole("admin");
        return BlockingOperationControl.isBlockingAllowed() && isAdmin;
    }

    @PermissionChecker("io-thread")
    boolean canRead_IOThread(SecurityIdentity identity) {
        if (failIfRequiredByHeader()) {
            return false;
        }
        boolean isAdmin = identity.hasRole("admin");
        if (!isAdmin) {
            return false;
        }
        return !BlockingOperationControl.isBlockingAllowed();
    }

    @PermissionChecker("io-thread-uni")
    Uni<Boolean> canRead_IOThread_Uni(SecurityIdentity identity) {
        if (failIfRequiredByHeader()) {
            return Uni.createFrom().item(false);
        }
        return Uni.createFrom().item(canRead_IOThread(identity));
    }

    @Blocking
    @PermissionChecker("worker-thread-method-args")
    boolean canRead_WorkerThread_SecuredMethodArg(SecurityContext securityContext) {
        if (failIfRequiredByHeader()) {
            return false;
        }
        boolean isAdmin = securityContext.isUserInRole("admin");
        return BlockingOperationControl.isBlockingAllowed() && isAdmin;
    }

    @PermissionChecker("io-thread-method-args")
    boolean canRead_IOThread_SecuredMethodArg(SecurityContext securityContext) {
        if (failIfRequiredByHeader()) {
            return false;
        }
        boolean isAdmin = securityContext.isUserInRole("admin");
        return !BlockingOperationControl.isBlockingAllowed() && isAdmin;
    }

    private boolean failIfRequiredByHeader() {
        return Boolean.parseBoolean(routingContext.request().getHeader("fail"));
    }
}
