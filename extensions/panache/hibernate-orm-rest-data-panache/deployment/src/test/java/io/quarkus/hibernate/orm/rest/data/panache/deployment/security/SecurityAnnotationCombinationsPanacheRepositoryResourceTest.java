package io.quarkus.hibernate.orm.rest.data.panache.deployment.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;

public class SecurityAnnotationCombinationsPanacheRepositoryResourceTest extends AbstractSecurityAnnotationTest {

    @ApplicationScoped
    public static class ItemsRepository implements PanacheRepository<Item> {
    }

    @RolesAllowed("admin")
    @ResourceProperties(path = "items")
    public interface ItemsRepositoryResource extends PanacheRepositoryResource<ItemsRepository, Item, Long> {

        @PermissionsAllowed("delete")
        boolean delete(Long id);

        @PermissionsAllowed("count-1")
        @PermissionsAllowed("count-2")
        @Override
        long count();
    }

    @ApplicationScoped
    public static class PiecesRepository implements PanacheRepository<Piece> {
    }

    @PermissionsAllowed("delete")
    @ResourceProperties(path = "pieces")
    public interface PiecesRepositoryResource extends PanacheRepositoryResource<PiecesRepository, Piece, Long> {

        @RolesAllowed("admin")
        boolean delete(Long id);
    }

    @ApplicationScoped
    public static class PermissionCheckerBean {

        @PermissionChecker("delete")
        boolean canDelete(SecurityIdentity identity) {
            return identity.hasRole("user");
        }

        @PermissionChecker("count-1")
        boolean canCount1(SecurityIdentity identity) {
            return identity.hasRole("user") || identity.hasRole("admin");
        }

        @PermissionChecker("count-2")
        boolean canCount2(SecurityIdentity identity) {
            return identity.hasRole("admin");
        }

    }

    @Test
    void testClassLevelSecurity() {
        // == Method generated for PanacheEntityResource
        // list method is protected so we should get an HTTP 401 if user is not authenticated
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
        // list method is protected so we should get an HTTP 200 if user doesn't have role 'user'
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
        // delete method is protected so we should get an HTTP 403 when the 'admin' role is missing
        given().auth().preemptive()
                .basic("foo", "foo")
                .accept("application/json")
                .when()
                .delete("/pieces/1")
                .then()
                .statusCode(403);
        // delete method is protected so we should get an HTTP 204 when the user has 'admin' role
        given().auth().preemptive()
                .basic("bar", "bar")
                .accept("application/json")
                .when()
                .delete("/pieces/1")
                .then()
                .statusCode(204);
    }

    @Test
    void testMethodLevelSecurity() {
        // == Method generated for PanacheEntityResource
        // list method is protected by class-level @RolesAllowed annotation
        given().accept("application/json")
                .when()
                .get("/items")
                .then()
                .statusCode(401);
        given().accept("application/json")
                .auth().preemptive().basic("foo", "foo")
                .when()
                .get("/items")
                .then()
                .statusCode(403);
        given().accept("application/json")
                .auth().preemptive().basic("bar", "bar")
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
