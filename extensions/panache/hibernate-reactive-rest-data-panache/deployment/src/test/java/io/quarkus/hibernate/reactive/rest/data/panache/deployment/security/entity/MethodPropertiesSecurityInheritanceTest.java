package io.quarkus.hibernate.reactive.rest.data.panache.deployment.security.entity;

import static io.restassured.RestAssured.given;

import java.util.List;

import jakarta.annotation.security.PermitAll;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.reactive.rest.data.panache.PanacheEntityResource;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity.AbstractEntity;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity.AbstractItem;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity.Collection;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity.Item;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

public class MethodPropertiesSecurityInheritanceTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, AbstractEntity.class, AbstractItem.class, Item.class,
                            InheritItemsResource.class, AuthItemsResource.class, OpenItemsResource.class,
                            AuthenticatedOpenItemsResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"))
            .overrideConfigKey("quarkus.security.users.embedded.enabled", "true")
            .overrideConfigKey("quarkus.security.users.embedded.plain-text", "true")
            .overrideConfigKey("quarkus.security.users.embedded.users.foo", "foo")
            .overrideConfigKey("quarkus.security.users.embedded.roles.foo", "user")
            .overrideConfigKey("quarkus.security.users.embedded.users.bar", "bar")
            .overrideConfigKey("quarkus.security.users.embedded.roles.bar", "admin")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-security-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment",
                            Version.getVersion())));

    // The whole resource is restricted to role "admin". Only the delete operation is customised, and only for its path.
    @ResourceProperties(path = "inherit-items", rolesAllowed = "admin")
    public interface InheritItemsResource extends PanacheEntityResource<Item, Long> {

        @MethodProperties(path = "custom")
        Uni<Boolean> delete(Long id);
    }

    // A method-level @Authenticated is a copied security annotation: it must govern the method and the build must
    // succeed (no "multiple security annotations" failure), even though the resource declares rolesAllowed.
    @ResourceProperties(path = "auth-items", rolesAllowed = "admin")
    public interface AuthItemsResource extends PanacheEntityResource<Item, Long> {

        @Authenticated
        Uni<Boolean> delete(Long id);
    }

    // The standard, self-documenting way to make a single operation public on an otherwise-secured resource.
    @ResourceProperties(path = "open-items", rolesAllowed = "admin")
    public interface OpenItemsResource extends PanacheEntityResource<Item, Long> {

        @PermitAll
        Uni<List<Item>> list(Page page, Sort sort);
    }

    // The resource requires authentication through `@ResourceProperties(authenticated = true)`. A method-level
    // @PermitAll must govern that operation instead; emitting the resource-level @Authenticated on top of the copied
    // annotation would make the generated method carry two security annotations, which the build rejects.
    @ResourceProperties(path = "auth-open-items", authenticated = true)
    public interface AuthenticatedOpenItemsResource extends PanacheEntityResource<Item, Long> {

        @PermitAll
        Uni<List<Item>> list(Page page, Sort sort);
    }

    @Test
    void pathCustomisationKeepsResourceLevelRoles() {
        // list() is not customised, so it inherits rolesAllowed = "admin": anonymous access is rejected.
        given().accept("application/json")
                .when().get("/inherit-items")
                .then().statusCode(401);

        // delete() was customised only with a path. It must STILL require role "admin".
        // Anonymous caller must be rejected (previously this discarded the roles and deleted the row).
        given().accept("application/json")
                .when().delete("/inherit-items/custom/1")
                .then().statusCode(401);

        // Authenticated but wrong role -> 403 (the role is enforced, not merely authentication).
        given().auth().preemptive().basic("foo", "foo")
                .accept("application/json")
                .when().delete("/inherit-items/custom/1")
                .then().statusCode(403);

        // Correct role -> 204, and the customised path is honoured (the feature still works).
        given().auth().preemptive().basic("bar", "bar")
                .accept("application/json")
                .when().delete("/inherit-items/custom/1")
                .then().statusCode(204);
    }

    @Test
    void methodLevelAuthenticatedGovernsAndBuildsSuccessfully() {
        // delete() carries a method-level @Authenticated on a resource that declares rolesAllowed = "admin".
        // The build must succeed and the endpoint must not be anonymous.
        given().accept("application/json")
                .when().delete("/auth-items/2")
                .then().statusCode(401);

        // The method-level @Authenticated governs, so any authenticated user is accepted.
        given().auth().preemptive().basic("foo", "foo")
                .accept("application/json")
                .when().delete("/auth-items/2")
                .then().statusCode(204);
    }

    @Test
    void permitAllOpensSingleOperationOnAuthenticatedResource() {
        // list() carries @PermitAll -> it is public even though the resource declares authenticated = true.
        given().accept("application/json")
                .when().get("/auth-open-items")
                .then().statusCode(200);

        // A sibling operation without its own method-level annotation still requires authentication.
        given().accept("application/json")
                .when().get("/auth-open-items/1")
                .then().statusCode(401);
    }

    @Test
    void permitAllOpensSingleOperationOnSecuredResource() {
        // list() carries @PermitAll -> it is public even though the resource requires role "admin".
        given().accept("application/json")
                .when().get("/open-items")
                .then().statusCode(200);

        // A sibling operation without its own method-level annotation still inherits rolesAllowed = "admin".
        given().accept("application/json")
                .when().get("/open-items/1")
                .then().statusCode(401);
    }
}
