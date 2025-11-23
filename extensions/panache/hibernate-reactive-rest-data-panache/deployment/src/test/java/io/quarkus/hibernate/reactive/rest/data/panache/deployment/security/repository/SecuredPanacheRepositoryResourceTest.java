package io.quarkus.hibernate.reactive.rest.data.panache.deployment.security.repository;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.AbstractEntity;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.AbstractItem;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.Collection;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.CollectionsRepository;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.EmptyListItem;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.EmptyListItemsRepository;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.Item;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.security.PermissionCheckerBean;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

public class SecuredPanacheRepositoryResourceTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, CollectionsRepository.class,
                            AbstractEntity.class, AbstractItem.class, Item.class,
                            EmptyListItem.class, EmptyListItemsRepository.class,
                            EmptyListItemsResource.class, PermissionCheckerBean.class)
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
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment", Version.getVersion())));

    @Test
    void defaultInterfaceMethodWithPermissionsAllowed() {
        // this endpoint requires permissions 'find-by-name-1' and 'find-by-name-2'
        // unauthenticated => 401
        given().accept("application/hal+json")
                .when().get("/collections/name/full collection")
                .then().statusCode(401);
        // 'find-by-name-1' => 403
        given().accept("application/hal+json")
                .auth().preemptive().basic("foo", "foo")
                .when().get("/collections/name/full collection")
                .then().statusCode(403);
        // 'find-by-name-1' && 'find-by-name-2' => 200
        given().accept("application/hal+json")
                .auth().preemptive().basic("bar", "bar")
                .when().get("/collections/name/full collection")
                .then().statusCode(200)
                .and().body("id", is("full"))
                .and().body("name", is("full collection"))
                .and().body("_links.addByName.href", containsString("/name/full"));
    }

    @Test
    void defaultInterfaceMethodWithRolesAllowed() {
        // authentication is required => expect HTTP status 401
        given().accept("application/json")
                .when().post("/collections/name/mycollection")
                .then().statusCode(401);
        // 'foo' has 'user' role, but 'admin' is required => expect HTTP status 403
        given().accept("application/json")
                .auth().preemptive().basic("foo", "foo")
                .when().post("/collections/name/mycollection")
                .then().statusCode(403);
        // 'bar' has 'admin' role => expect HTTP status 200
        given().accept("application/json")
                .auth().preemptive().basic("bar", "bar")
                .when().post("/collections/name/mycollection")
                .then().statusCode(200)
                .and().body("id", is("mycollection"))
                .and().body("name", is("mycollection"));
    }

    @Test
    void explicitlyDeclaredMethodWithPermissionsAllowed() {
        // this endpoint requires permissions 'get-1' and 'get-2'
        // unauthenticated => 401
        given().accept("application/json")
                .when().get("/collections/full")
                .then().statusCode(401);
        // permission 'get-1' => 403
        given().accept("application/json")
                .auth().preemptive().basic("foo", "foo")
                .when().get("/collections/full")
                .then().statusCode(403);
        // permission 'get-1' && 'get-2' => 200
        given().accept("application/json")
                .auth().preemptive().basic("bar", "bar")
                .when().get("/collections/full")
                .then().statusCode(200)
                .and().body("id", is(equalTo("full")))
                .and().body("name", is(equalTo("full collection")))
                .and().body("items.id", contains(1, 2))
                .and().body("items.name", contains("first", "second"));
    }

    @Test
    void testClassLevelPermissionsAllowed() {
        // authentication required => expect HTTP status 401
        given().accept("application/hal+json")
                .and().queryParam("page", 1)
                .and().queryParam("size", 1)
                .when().get("/empty-list-items")
                .then().statusCode(401);
        // permission 'list-empty' is required => expect HTTP status 403
        given().accept("application/hal+json")
                .auth().preemptive().basic("foo", "foo")
                .and().queryParam("page", 1)
                .and().queryParam("size", 1)
                .when().get("/empty-list-items")
                .then().statusCode(403);
        given().accept("application/hal+json")
                .auth().preemptive().basic("bar", "bar")
                .and().queryParam("page", 1)
                .and().queryParam("size", 1)
                .when().get("/empty-list-items")
                .then().statusCode(200);
    }

    @Test
    void testSecurityCheckBeforeSerialization() {
        given().accept("application/json")
                .contentType("application/json")
                .body("@$%*()^")
                .when().post("/collections")
                .then().statusCode(401);
        given().accept("application/json")
                .auth().preemptive().basic("foo", "foo")
                .contentType("application/json")
                .body("@$%*()^")
                .when().post("/collections")
                .then().statusCode(403);
        given().accept("application/json")
                .auth().preemptive().basic("bar", "bar")
                .contentType("application/json")
                .body("@$%*()^")
                .when().post("/collections")
                .then().statusCode(500);
        var collection = new Collection();
        collection.name = "mine";
        collection.id = "whatever";
        given().accept("application/json")
                .auth().preemptive().basic("bar", "bar")
                .contentType("application/json")
                .body(collection)
                .when().post("/collections")
                .then().statusCode(201);
    }
}
