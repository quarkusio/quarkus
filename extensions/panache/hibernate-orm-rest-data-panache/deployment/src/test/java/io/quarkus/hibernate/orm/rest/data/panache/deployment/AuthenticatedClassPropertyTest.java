package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusUnitTest;

public class AuthenticatedClassPropertyTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Item.class, ItemsResource.class)
                    .addAsResource(new StringAsset(
                            """
                                            insert into item(id, name) values (1, 'first');
                                            insert into item(id, name) values (2, 'second');
                                    """),
                            "import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-h2-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-rest-jackson-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.security.users.embedded.enabled", "true")
            .overrideConfigKey("quarkus.security.users.embedded.plain-text", "true")
            .overrideConfigKey("quarkus.security.users.embedded.users.foo", "foo")
            .overrideConfigKey("quarkus.security.users.embedded.roles.foo", "user")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:test")
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create");

    @Entity
    @Table(name = "item")
    public static class Item extends PanacheEntity {

        public String name;
    }

    @ResourceProperties(path = "items", authenticated = true)
    public interface ItemsResource extends PanacheEntityResource<Item, Long> {

        @Authenticated // we add it just to make sure that adding it doesn't break the generated code
        boolean delete(Long id);
    }

    @Test
    void test() {
        // list method is protected so we should get an HTTP 200 even if no user is specified
        given().accept("application/json")
                .when()
                .get("/items")
                .then()
                .statusCode(401);

        // count method is protected so we should get an HTTP 200 even if no user is specified
        given().accept("application/json")
                .when()
                .get("/items/count")
                .then()
                .statusCode(401);

        // delete method is also protected so we should get an HTTP 401 when no user is specified
        given().accept("application/json")
                .when()
                .delete("/items/1")
                .then()
                .statusCode(401);

        // list method is protected so we should get an HTTP 401 when a wrong username and password is specified
        given().auth().preemptive()
                .basic("foo", "foo2")
                .accept("application/json")
                .when()
                .delete("/items")
                .then()
                .statusCode(401);

        // list method is protected so we should get an HTTP 200 when the proper username and password are specified
        given().auth().preemptive()
                .basic("foo", "foo")
                .accept("application/json")
                .when()
                .get("/items")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));
    }
}
