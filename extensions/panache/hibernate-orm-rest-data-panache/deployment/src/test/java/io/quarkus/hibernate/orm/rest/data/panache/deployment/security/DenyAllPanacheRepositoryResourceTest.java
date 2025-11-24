package io.quarkus.hibernate.orm.rest.data.panache.deployment.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.rest.data.panache.ResourceProperties;

public class DenyAllPanacheRepositoryResourceTest extends AbstractSecurityAnnotationTest {

    @ApplicationScoped
    public static class ItemsRepository implements PanacheRepository<Item> {
    }

    @ResourceProperties(path = "items")
    public interface ItemsRepositoryResource extends PanacheRepositoryResource<ItemsRepository, Item, Long> {

        @DenyAll
        boolean delete(Long id);

        long count();
    }

    @ApplicationScoped
    public static class PiecesRepository implements PanacheRepository<Piece> {
    }

    @DenyAll
    @ResourceProperties(path = "pieces")
    public interface PiecesRepositoryResource extends PanacheRepositoryResource<PiecesRepository, Piece, Long> {

        @PermitAll
        boolean delete(Long id);

        long count();
    }

    @Test
    void testClassLevelSecurity() {
        // == Method generated for PanacheRepositoryResource annotated with @DenyAll
        // list method is protected so we should get an HTTP 401 if user is not authenticated
        given().accept("application/json")
                .when()
                .get("/pieces")
                .then()
                .statusCode(401);
        // list method is protected so we should get an HTTP 403 if user is authenticated
        given().accept("application/json")
                .when()
                .auth().preemptive().basic("foo", "foo")
                .get("/pieces")
                .then()
                .statusCode(403);

        // == Explicitly declared resource method annotated with method-level @PermitAll
        // delete method is not protected so we should get an HTTP 204
        given()
                .accept("application/json")
                .when()
                .delete("/pieces/1")
                .then()
                .statusCode(204);

        // == Explicitly declared resource method unannotated, therefore we expect that @DenyAll is applied
        given()
                .accept("application/json")
                .when()
                .get("/pieces/count")
                .then()
                .statusCode(401);
        given().auth().preemptive()
                .basic("bar", "bar")
                .accept("application/json")
                .when()
                .get("/pieces/count")
                .then()
                .statusCode(403);
    }

    @Test
    void testMethodLevelSecurity() {
        // == Method generated for PanacheRepositoryResource
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
        // delete method is protected so we should get an HTTP 403 for authenticated user
        given().auth().preemptive()
                .basic("bar", "bar")
                .accept("application/json")
                .when()
                .delete("/items/1")
                .then()
                .statusCode(403);

        // == Method explicitly declared on PanacheRepositoryResource
        // unauthenticated user has access to unannotated method
        given()
                .accept("application/json")
                .when()
                .get("/items/count")
                .then()
                .statusCode(200);
    }

}
