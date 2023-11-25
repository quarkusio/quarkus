package io.quarkus.vertx.http.security.permission;

import static io.quarkus.vertx.http.security.permission.AbstractHttpSecurityPolicyGrantingPermissionsTest.AuthenticatedUserImpl.ADMIN;
import static io.quarkus.vertx.http.security.permission.AbstractHttpSecurityPolicyGrantingPermissionsTest.AuthenticatedUserImpl.ROOT;
import static io.quarkus.vertx.http.security.permission.AbstractHttpSecurityPolicyGrantingPermissionsTest.AuthenticatedUserImpl.USER;

import java.util.function.Supplier;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.vertx.http.security.CustomPermission;
import io.quarkus.vertx.http.security.CustomPermissionWithActions;
import io.quarkus.vertx.http.security.permission.AbstractHttpSecurityPolicyGrantingPermissionsTest.AuthenticatedUser;
import io.quarkus.vertx.http.security.permission.AbstractHttpSecurityPolicyGrantingPermissionsTest.AuthenticatedUserImpl;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HttpSecPolicyGrantingRolesTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addClasses(TestIdentityController.class, TestIdentityProvider.class, RolesPathHandler.class,
                    CDIBean.class, CustomPermission.class, CustomPermissionWithActions.class, AuthenticatedUser.class,
                    AuthenticatedUserImpl.class)
            .addAsResource("conf/http-roles-grant-config.properties", "application.properties"));

    @Test
    public void mapRolesToRolesSecuredWithRolesAllowed() {
        assertSuccess(ADMIN, "/test/new-admin-roles-blocking");
        assertSuccess(ADMIN, "/test/new-admin-roles");
        assertSuccess(ADMIN, "/test/new-admin-roles2");
        assertSuccess(ADMIN, "/test/old-admin-roles-blocking");
        assertSuccess(ADMIN, "/test/old-admin-roles");
        assertSuccess(ADMIN, "/test/multiple-new-roles-1");
        assertSuccess(ADMIN, "/test/multiple-new-roles-2");
        assertSuccess(ADMIN, "/test/multiple-new-roles-3");
        assertForbidden(USER, "/test/new-admin-roles-blocking");
        assertForbidden(USER, "/test/new-admin-roles");
        assertForbidden(USER, "/test/new-admin-roles2");
        assertForbidden(USER, "/test/old-admin-roles-blocking");
        assertForbidden(USER, "/test/old-admin-roles");
        assertSuccess(USER, "/test/multiple-new-roles-1");
        assertSuccess(USER, "/test/multiple-new-roles-2");
        assertSuccess(USER, "/test/multiple-new-roles-3");
    }

    @Test
    public void mapRolesToRolesNoSecurityAnnotation() {
        assertSuccess(ADMIN, "/test/roles-allowed-path");
        assertForbidden(USER, "/test/roles-allowed-path");
        assertForbidden(ROOT, "/test/roles-allowed-path");
        assertSuccess(ADMIN, "/test/granted-and-checked-by-policy");
        assertForbidden(USER, "/test/granted-and-checked-by-policy");
        assertSuccess(ROOT, "/test/granted-and-checked-by-policy");
    }

    @Test
    public void mapRolesToBothPermissionsAndRoles() {
        assertSuccess(ADMIN, "/test/roles-and-perms-1");
        assertSuccess(ADMIN, "/test/roles-and-perms-2");
        assertForbidden(USER, "/test/roles-and-perms-1");
        assertForbidden(USER, "/test/roles-and-perms-2");
    }

    @ApplicationScoped
    public static class RolesPathHandler {

        @Inject
        CDIBean cdiBean;

        public void setup(@Observes Router router) {
            router.route("/test/new-admin-roles-blocking").blockingHandler(new RouteHandler(() -> {
                cdiBean.newRolesBlocking();
                return Uni.createFrom().nullItem();
            }));
            router.route("/test/new-admin-roles").handler(new RouteHandler(cdiBean::newRoles));
            router.route("/test/new-admin-roles2").handler(new RouteHandler(cdiBean::newRoles2));
            router.route("/test/old-admin-roles-blocking").blockingHandler(new RouteHandler(() -> {
                cdiBean.oldRolesBlocking();
                return Uni.createFrom().nullItem();
            }));
            router.route("/test/old-admin-roles").handler(new RouteHandler(cdiBean::oldRoles));
            router.route("/test/multiple-new-roles-1").handler(new RouteHandler(cdiBean::multipleNewRoles1));
            router.route("/test/multiple-new-roles-2").handler(new RouteHandler(cdiBean::multipleNewRoles2));
            router.route("/test/multiple-new-roles-3").handler(new RouteHandler(cdiBean::multipleNewRoles3));
            router.route("/test/roles-allowed-path").handler(new RouteHandler(cdiBean::checkAdmin1Role));
            router.route("/test/granted-and-checked-by-policy").handler(new RouteHandler(cdiBean::checkAdmin1Role));
            router.route("/test/roles-and-perms-1").handler(new RouteHandler(cdiBean::rolesAndPermissions1));
            router.route("/test/roles-and-perms-2").handler(new RouteHandler(cdiBean::rolesAndPermissions2));
        }
    }

    private static final class RouteHandler implements Handler<RoutingContext> {

        private final Supplier<Uni<Void>> callService;

        private RouteHandler(Supplier<Uni<Void>> callService) {
            this.callService = callService;
        }

        @Override
        public void handle(RoutingContext event) {
            // activate context so that we can use CDI beans
            Arc.container().requestContext().activate();
            // set identity used by security checks performed by standard security interceptors
            QuarkusHttpUser user = (QuarkusHttpUser) event.user();
            Arc.container().instance(SecurityIdentityAssociation.class).get().setIdentity(user.getSecurityIdentity());

            callService.get().subscribe().with(unused -> {
                String ret = user.getSecurityIdentity().getPrincipal().getName() +
                        ":" + event.normalizedPath();
                event.response().end(ret);
            }, throwable -> {
                if (throwable instanceof UnauthorizedException) {
                    event.response().setStatusCode(401);
                } else if (throwable instanceof ForbiddenException) {
                    event.response().setStatusCode(403);
                } else {
                    event.response().setStatusCode(500);
                }
                event.end();
            });
        }
    }

    private void assertSuccess(AuthenticatedUser user, String... paths) {
        user.authenticate();
        for (var path : paths) {
            RestAssured
                    .given()
                    .auth()
                    .basic(user.role(), user.role())
                    .get(path)
                    .then()
                    .statusCode(200)
                    .body(Matchers.is(user.role() + ":" + path));
        }
    }

    private void assertForbidden(AuthenticatedUser user, String... paths) {
        user.authenticate();
        for (var path : paths) {
            RestAssured
                    .given()
                    .auth()
                    .basic(user.role(), user.role())
                    .get(path)
                    .then()
                    .statusCode(403);
        }
    }

    @ApplicationScoped
    public static class CDIBean {

        @Inject
        SecurityIdentity identity;

        @RolesAllowed("Admin1")
        public void newRolesBlocking() {
            // NOTHING TO DO
        }

        @RolesAllowed("Admin1")
        public Uni<Void> newRoles() {
            return Uni.createFrom().nullItem();
        }

        @RolesAllowed("Admin2")
        public Uni<Void> newRoles2() {
            return Uni.createFrom().nullItem();
        }

        @RolesAllowed("admin")
        public void oldRolesBlocking() {
            // NOTHING TO DO
        }

        @RolesAllowed("admin")
        public Uni<Void> oldRoles() {
            return Uni.createFrom().nullItem();
        }

        @RolesAllowed("Janet")
        public Uni<Void> multipleNewRoles1() {
            return Uni.createFrom().nullItem();
        }

        @RolesAllowed("Monica")
        public Uni<Void> multipleNewRoles2() {
            return Uni.createFrom().nullItem();
        }

        @RolesAllowed("Robin")
        public Uni<Void> multipleNewRoles3() {
            return Uni.createFrom().nullItem();
        }

        @RolesAllowed("Admin3")
        public Uni<Void> rolesAndPermissions1() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed("jump")
        public Uni<Void> rolesAndPermissions2() {
            return Uni.createFrom().nullItem();
        }

        public Uni<Void> checkAdmin1Role() {
            if (identity.hasRole("Admin1")) {
                if (identity.getPrincipal().getName().equals("root")) {
                    if (identity.hasRole("sudo")) {
                        return Uni.createFrom().nullItem();
                    }
                } else {
                    return Uni.createFrom().nullItem();
                }
            }
            return Uni.createFrom().failure(AuthenticationFailedException::new);
        }
    }
}
