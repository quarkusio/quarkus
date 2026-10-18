package io.quarkus.hibernate.orm.rest.data.panache.deployment.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

public class PermissionsAllowedPanacheResourceTest extends AbstractSecurityAnnotationTest {

    @ResourceProperties(path = "items")
    public interface ItemsResource extends PanacheEntityResource<Item, Long> {

        @PermissionsAllowed("delete")
        boolean delete(Long id);

        @PermissionsAllowed("count-1")
        @PermissionsAllowed("count-2")
        @Override
        long count();
    }

    @PermissionsAllowed("delete")
    @ResourceProperties(path = "pieces")
    public interface PiecesResource extends PanacheEntityResource<Piece, Long> {

        boolean delete(Long id);
    }

    @ApplicationScoped
    public static class PermissionsAugmentor implements SecurityIdentityAugmentor {

        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
                AuthenticationRequestContext authenticationRequestContext) {
            if (securityIdentity != null && !securityIdentity.isAnonymous()) {
                if (securityIdentity.hasRole("admin")) {
                    return Uni.createFrom().item(QuarkusSecurityIdentity
                            .builder(securityIdentity)
                            .addPermissionAsString("count-1")
                            .addPermissionAsString("count-2")
                            .build());
                }
                if (securityIdentity.hasRole("user")) {
                    return Uni.createFrom().item(QuarkusSecurityIdentity
                            .builder(securityIdentity)
                            .addPermissionAsString("delete")
                            .addPermissionAsString("count-1")
                            .build());
                }
            }
            return Uni.createFrom().item(securityIdentity);
        }
    }

    @Test
    void testClassLevelSecurity() {
        // == Method generated for PanacheEntityResource
        // list method is protected so we should get an HTTP 401 even if user is not authenticated
        given().accept("application/json")
                .when()
                .get("/pieces")
                .then()
                .statusCode(401);
        // list method is protected so we should get an HTTP 403 if user doesn't have permission 'delete'
        given().accept("application/json")
                .when()
                .auth().preemptive().basic("bar", "bar")
                .get("/pieces")
                .then()
                .statusCode(403);
        // list method is protected so we should get an HTTP 200 if user has permission 'delete'
        given().accept("application/json")
                .when()
                .auth().preemptive().basic("foo", "foo")
                .get("/pieces")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));

        // == Explicitly declared resource method
        // delete method is protected so we should get an HTTP 401 when no user is specified
        given().accept("application/json")
                .when()
                .delete("/pieces/1")
                .then()
                .statusCode(401);
        // delete method is protected so we should get an HTTP 401 when a wrong username and password is specified
        given().auth().preemptive()
                .basic("foo", "foo2")
                .accept("application/json")
                .when()
                .delete("/pieces/1")
                .then()
                .statusCode(401);
        // delete method is protected so we should get an HTTP 403 when the 'delete' permission is missing
        given().auth().preemptive()
                .basic("bar", "bar")
                .accept("application/json")
                .when()
                .delete("/pieces/1")
                .then()
                .statusCode(403);
        // delete method is protected so we should get an HTTP 204 when the 'delete' permission is present
        given().auth().preemptive()
                .basic("foo", "foo")
                .accept("application/json")
                .when()
                .delete("/pieces/1")
                .then()
                .statusCode(204);
    }

    @Test
    void testMethodLevelSecurity() {
        // == Method generated for PanacheEntityResource
        // list method is not protected so we should get an HTTP 200 even if no user is specified
        given().accept("application/json")
                .when()
                .get("/items")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));

        // == Explicitly declared resource method
        // delete method is protected so we should get an HTTP 401 when no user is specified
        given().accept("application/json")
                .when()
                .delete("/items/1")
                .then()
                .statusCode(401);
        // delete method is protected so we should get an HTTP 401 when a wrong username and password is specified
        given().auth().preemptive()
                .basic("foo", "foo2")
                .accept("application/json")
                .when()
                .delete("/items/1")
                .then()
                .statusCode(401);
        // delete method is protected so we should get an HTTP 403 when the 'delete' permission is missing
        given().auth().preemptive()
                .basic("bar", "bar")
                .accept("application/json")
                .when()
                .delete("/items/1")
                .then()
                .statusCode(403);
        // delete method is protected so we should get an HTTP 204 when the 'delete' permission is present
        given().auth().preemptive()
                .basic("foo", "foo")
                .accept("application/json")
                .when()
                .delete("/items/1")
                .then()
                .statusCode(204);

        // == Method generated for PanacheEntityResource
        // test repeated annotations
        // unauthenticated user has neither of required permissions
        given()
                .accept("application/json")
                .when()
                .get("/items/count")
                .then()
                .statusCode(401);
        // the 'foo' only has one of required permissions
        given().auth().preemptive()
                .basic("foo", "foo")
                .accept("application/json")
                .when()
                .get("/items/count")
                .then()
                .statusCode(403);
        // the 'bar' user has both repeated permissions
        given().auth().preemptive()
                .basic("bar", "bar")
                .accept("application/json")
                .when()
                .get("/items/count")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

}
