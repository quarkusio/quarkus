package io.quarkus.hibernate.orm.rest.data.panache.deployment.security;

import static io.restassured.RestAssured.given;

import java.util.List;

import jakarta.annotation.security.PermitAll;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.security.Authenticated;

public class MethodPropertiesSecurityInheritanceTest extends AbstractSecurityAnnotationTest {

    // The whole resource is restricted to role "admin". Only the delete operation is customised, and only for its path.
    @ResourceProperties(path = "pieces", rolesAllowed = "admin")
    public interface PiecesResource extends PanacheEntityResource<Piece, Long> {

        @MethodProperties(path = "custom")
        boolean delete(Long id);
    }

    // A method-level @Authenticated is a copied security annotation: it must govern the method and the build must
    // succeed (no "multiple security annotations" failure), even though the resource declares rolesAllowed.
    @ResourceProperties(path = "items", rolesAllowed = "admin")
    public interface ItemsResource extends PanacheEntityResource<Item, Long> {

        @Authenticated
        boolean delete(Long id);
    }

    // The standard, self-documenting way to make a single operation public on an otherwise-secured resource:
    // a method-level @PermitAll (a copied security annotation) governs that operation.
    @ResourceProperties(path = "openpieces", rolesAllowed = "admin")
    public interface OpenPiecesResource extends PanacheEntityResource<Piece, Long> {

        @PermitAll
        List<Piece> list(Page page, Sort sort);
    }

    // The resource requires authentication through `@ResourceProperties(authenticated = true)`. A method-level
    // @PermitAll must govern that operation instead; emitting the resource-level @Authenticated on top of the copied
    // annotation would make the generated method carry two security annotations, which the build rejects.
    @ResourceProperties(path = "authpieces", authenticated = true)
    public interface AuthenticatedPiecesResource extends PanacheEntityResource<Piece, Long> {

        @PermitAll
        List<Piece> list(Page page, Sort sort);
    }

    @Test
    void pathCustomisationKeepsResourceLevelRoles() {
        // list() is not customised, so it inherits rolesAllowed = "admin": anonymous access is rejected.
        given().accept("application/json")
                .when().get("/pieces")
                .then().statusCode(401);

        // delete() was customised only with a path. It must STILL require role "admin".
        // Anonymous caller must be rejected (previously this returned 204 and deleted the row).
        given().accept("application/json")
                .when().delete("/pieces/custom/1")
                .then().statusCode(401);

        // Authenticated but wrong role -> 403 (the role is enforced, not merely authentication).
        given().auth().preemptive().basic("foo", "foo")
                .accept("application/json")
                .when().delete("/pieces/custom/1")
                .then().statusCode(403);

        // Correct role -> 204, and the customised path is honoured (the feature still works).
        given().auth().preemptive().basic("bar", "bar")
                .accept("application/json")
                .when().delete("/pieces/custom/1")
                .then().statusCode(204);
    }

    @Test
    void methodLevelAuthenticatedGovernsAndBuildsSuccessfully() {
        // delete() carries a method-level @Authenticated on a resource that declares rolesAllowed = "admin".
        // The build must succeed and the endpoint must not be anonymous.
        given().accept("application/json")
                .when().delete("/items/1")
                .then().statusCode(401);

        // The method-level @Authenticated governs, so any authenticated user is accepted.
        given().auth().preemptive().basic("foo", "foo")
                .accept("application/json")
                .when().delete("/items/1")
                .then().statusCode(204);
    }

    @Test
    void permitAllOpensSingleOperationOnAuthenticatedResource() {
        // list() carries @PermitAll -> it is public even though the resource declares authenticated = true.
        given().accept("application/json")
                .when().get("/authpieces")
                .then().statusCode(200);

        // A sibling operation without its own method-level annotation still requires authentication.
        given().accept("application/json")
                .when().get("/authpieces/1")
                .then().statusCode(401);
    }

    @Test
    void permitAllOpensSingleOperationOnSecuredResource() {
        // list() carries @PermitAll -> it is public even though the resource requires role "admin".
        given().accept("application/json")
                .when().get("/openpieces")
                .then().statusCode(200);

        // A sibling operation without its own method-level annotation still inherits rolesAllowed = "admin".
        given().accept("application/json")
                .when().get("/openpieces/1")
                .then().statusCode(401);
    }
}
