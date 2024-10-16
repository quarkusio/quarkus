package io.quarkus.vertx.http.security.permission;

import static io.quarkus.vertx.http.security.permission.AbstractHttpSecurityPolicyGrantingPermissionsTest.AuthenticatedUserImpl.ADMIN;
import static io.quarkus.vertx.http.security.permission.AbstractHttpSecurityPolicyGrantingPermissionsTest.AuthenticatedUserImpl.TEST;
import static io.quarkus.vertx.http.security.permission.AbstractHttpSecurityPolicyGrantingPermissionsTest.AuthenticatedUserImpl.TEST2;
import static io.quarkus.vertx.http.security.permission.AbstractHttpSecurityPolicyGrantingPermissionsTest.AuthenticatedUserImpl.USER;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.StringPermission;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.vertx.http.security.CustomPermission;
import io.quarkus.vertx.http.security.CustomPermissionWithActions;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public abstract class AbstractHttpSecurityPolicyGrantingPermissionsTest {

    @Test
    public void grantSingleStringPermissionWithoutAction() {
        // only user with permission 'create-test' is allowed to proceed
        // we mapped role 'test' to 'create-test' on path '/test/create'
        // path '/test/create' is only accessible to requests with role 'test'
        assertSuccess(TEST, "/test/create");

        // 'test2' has permission granted by the same policy as 'test' role
        assertSuccess(TEST2, "/test/create");

        // admin role has access to '/test/create' path, but no 'create-test' permission
        assertForbidden(ADMIN, "/test/create");

        // user role has no access to '/test/create' path
        assertForbidden(USER, "/test/create");

        // we don't grant permission via HTTP Security Policy, but directly to the identity
        assertSuccess(withOtherPermissions("create-test"), "/test/create");

        // we grant wrong permission directly to the identity
        assertForbidden(withOtherPermissions("wrong-permission"), "/test/create");

        // unauthenticated request must be denied
        assertUnauthorized("/test/create");
    }

    @Test
    public void grantMultipleStringPermissionsSomeWithActions() {
        // assert permissions granted by other policy don't affect other policies
        // that is we require 'create-test' permission granted to role 'test' on path '/test/create'
        // but not on path '/test/create2'; path "/test/update2" must be forbidden as permission is missing action
        assertForbidden(TEST, "/test/create2", "/test/update2");
        // role 'test' must have access to all the policy paths
        assertSuccess(TEST, "/test/list", "/test/update", "/test/delete");

        // role 'test2' must have 2 paths (list, update, update2), but not other paths
        assertSuccess(TEST2, "/test/list", "/test/update", "/test/update2", "/test/create");
        assertForbidden(TEST2, "/test/delete", "/test/create2");

        // role 'admin' must be denied access to '/test/delete' as the admin is missing required action
        assertForbidden(ADMIN, "/test/delete", "/test/list", "/test/update", "/test/create2");

        // we don't grant permission via HTTP Security Policy, but directly to the identity
        assertSuccess(withOtherPermissions("list-test"), "/test/list");
        assertForbidden(withOtherPermissions("list-test"), "/test/update");
        assertSuccess(withOtherPermissions("update-test"), "/test/update");
        // path '/test/update2' must be denied as the role is missing required action
        assertForbidden(withOtherPermissions("update-test"), "/test/list", "/test/update2");
    }

    @Test
    public void grantSingleCustomPermission() {
        assertSuccess(TEST, "/test/custom");

        // fail as has no permission (but has granted path access)
        assertForbidden(TEST2, "/test/custom");

        // fail as has different permission
        assertForbidden(ADMIN, "/test/custom");
    }

    @Test
    public void grantMultiCustomPermissionsSomeWithActions() {
        // role 'test' has correct permissions without any actions
        assertForbidden(TEST, "/test/custom-action");
        // role 'test2' has correct permissions with wrong actions
        assertForbidden(TEST2, "/test/custom-action");
        // role 'user' has correct permissions and actions required by 1st annotation, but miss actions required by 2nd one
        assertForbidden(USER, "/test/custom-action");
        // role 'admin' has at least one correct permission and action required by both annotations
        assertSuccess(ADMIN, "/test/custom-action");
    }

    @Test
    public void grantStringPermissionToAnyAuthenticatedReq() {
        // any authenticated user is allowed is granted HTTP permission to access path '/test/authenticated'
        assertSuccess(ADMIN, "/test/authenticated");
        assertSuccess(USER, "/test/authenticated");
        assertSuccess(TEST, "/test/authenticated");
        assertSuccess(TEST2, "/test/authenticated");
        assertUnauthorized("/test/authenticated");

        // any authenticated user is granted HTTP permission to access path '/test/authenticated-admin',
        // but only admin is granted 'auth-admin-perm' permission
        assertSuccess(ADMIN, "/test/authenticated-admin");
        assertForbidden(USER, "/test/authenticated-admin");
        assertForbidden(TEST, "/test/authenticated-admin");
        assertUnauthorized("/test/authenticated-admin");

        // any authenticated user is granted HTTP permission to access path '/test/authenticated-user',
        // but only user is granted 'auth-user-perm' permission
        assertSuccess(USER, "/test/authenticated-user");
        assertForbidden(ADMIN, "/test/authenticated-user");

        // any authenticated user is granted HTTP permission to access path '/test/authenticated-test-role',
        // but only test role is granted both 'auth-test-perm1' and 'auth-test-perm2' permissions
        assertSuccess(TEST, "/test/authenticated-test-role");
        // role 'test2' has only one of two required permissions
        assertForbidden(TEST2, "/test/authenticated-test-role");
        // admin has no permissions
        assertForbidden(ADMIN, "/test/authenticated-test-role");
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

    private void assertUnauthorized(String path) {
        RestAssured
                .given()
                .get(path)
                .then()
                .statusCode(401);
    }

    @ApplicationScoped
    public static class PermissionsPathHandler {

        @Inject
        CDIBean cdiBean;

        public void setup(@Observes Router router) {
            router.route("/test/create").blockingHandler(new RouteHandler(() -> {
                cdiBean.createTestBlocking();
                return Uni.createFrom().nullItem();
            }));
            router.route("/test/create2").handler(new RouteHandler(cdiBean::createTest));
            router.route("/test/delete").handler(new RouteHandler(cdiBean::deleteTest));
            router.route("/test/update").handler(new RouteHandler(cdiBean::updateTest));
            router.route("/test/update2").handler(new RouteHandler(cdiBean::update2Test));
            router.route("/test/list").handler(new RouteHandler(cdiBean::listTest));
            router.route("/test/custom").handler(new RouteHandler(cdiBean::customTest));
            router.route("/test/custom-action").handler(new RouteHandler(cdiBean::customActionsTest));
            router.route("/test/authenticated").handler(new RouteHandler(cdiBean::authenticatedTest));
            router.route("/test/authenticated-admin").handler(new RouteHandler(cdiBean::authenticatedAdminTest));
            router.route("/test/authenticated-user").handler(new RouteHandler(cdiBean::authenticatedUserTest));
            router.route("/test/authenticated-test-role").handler(new RouteHandler(cdiBean::authenticatedTestRoleTest));
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

    @ApplicationScoped
    public static class CDIBean {

        @PermissionsAllowed("create-test")
        public void createTestBlocking() {
            // NOTHING TO DO
        }

        @PermissionsAllowed("create-test")
        public Uni<Void> createTest() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed("update-test")
        public Uni<Void> updateTest() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed("update-test:action2")
        public Uni<Void> update2Test() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed("list-test")
        public Uni<Void> listTest() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed("delete-test:action1")
        public Uni<Void> deleteTest() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed(value = "custom-test", permission = CustomPermission.class)
        public Uni<Void> customTest() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed(value = { "custom-actions-test", "custom-actions-test1:action9",
                "custom-actions-test2:action9" }, permission = CustomPermissionWithActions.class)
        @PermissionsAllowed(value = { "custom-actions-test1:action7",
                "custom-actions-test1:action8" }, permission = CustomPermissionWithActions.class)
        public Uni<Void> customActionsTest() {
            return Uni.createFrom().nullItem();
        }

        public Uni<Void> authenticatedTest() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed("auth-admin-perm")
        public Uni<Void> authenticatedAdminTest() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed("auth-user-perm")
        public Uni<Void> authenticatedUserTest() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed("auth-test-perm2")
        @PermissionsAllowed("auth-test-perm1")
        public Uni<Void> authenticatedTestRoleTest() {
            return Uni.createFrom().nullItem();
        }
    }

    interface AuthenticatedUser {

        String role();

        void authenticate();

    }

    enum AuthenticatedUserImpl implements AuthenticatedUser {
        ADMIN(AuthenticatedUserImpl::useAdminRole),
        ROOT(AuthenticatedUserImpl::useRootRole),
        USER(AuthenticatedUserImpl::useUserRole),
        TEST(AuthenticatedUserImpl::useTestRole),
        TEST2(AuthenticatedUserImpl::useTest2Role);

        private final Runnable authenticate;

        AuthenticatedUserImpl(Runnable authenticate) {
            this.authenticate = authenticate;
        }

        public void authenticate() {
            authenticate.run();
        }

        public String role() {
            return this.toString().toLowerCase();
        }

        private static void useTestRole() {
            TestIdentityController.resetRoles().add("test", "test", "test");
        }

        private static void useTest2Role() {
            TestIdentityController.resetRoles().add("test2", "test2", "test2");
        }

        private static void useRootRole() {
            TestIdentityController.resetRoles().add("root", "root", "root", "Admin1");
        }

        private static void useAdminRole() {
            TestIdentityController.resetRoles().add("admin", "admin", "admin");
        }

        private static void useUserRole() {
            TestIdentityController.resetRoles().add("user", "user", "user");
        }

    }

    private static AuthenticatedUser withOtherPermissions(String permissionName) {
        return new AuthenticatedUser() {

            private static final String OTHER = "other";

            @Override
            public String role() {
                return OTHER;
            }

            @Override
            public void authenticate() {
                // we grant additional permissions to the user directly
                TestIdentityController.resetRoles().add(OTHER, OTHER, new StringPermission(permissionName));
            }
        };
    }
}
