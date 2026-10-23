package io.quarkus.hibernate.orm.rest.data.panache.deployment.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.quarkus.test.QuarkusExtensionTest;

class SecureFieldSortAndFilterTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FieldEmployee.class, GetterEmployee.class, FieldEmployeesResource.class,
                            GetterEmployeesResource.class)
                    .addAsResource(new StringAsset(
                            """
                                    insert into field_employee(id, name, salary) values (1, 'Alice', 10000);
                                    insert into field_employee(id, name, salary) values (2, 'Bob', 30000);
                                    insert into field_employee(id, name, salary) values (3, 'Carol', 20000);
                                    insert into getter_employee(id, name, salary) values (1, 'Alice', 10000);
                                    insert into getter_employee(id, name, salary) values (2, 'Bob', 30000);
                                    insert into getter_employee(id, name, salary) values (3, 'Carol', 20000);
                                    """),
                            "import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-h2-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-rest-jackson-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.security.users.embedded.enabled", "true")
            .overrideConfigKey("quarkus.security.users.embedded.plain-text", "true")
            .overrideConfigKey("quarkus.security.users.embedded.users.viewer", "viewer")
            .overrideConfigKey("quarkus.security.users.embedded.roles.viewer", "user")
            .overrideConfigKey("quarkus.security.users.embedded.users.admin", "admin")
            .overrideConfigKey("quarkus.security.users.embedded.roles.admin", "admin");

    @ParameterizedTest
    @ValueSource(strings = { "field-employees", "getter-employees" })
    void sortBySecureFieldRequiresRole(String path) {
        given().accept("application/json")
                .when().get("/" + path + "?sort=-salary")
                .then().statusCode(400);
        given().accept("application/json")
                .auth().preemptive().basic("viewer", "viewer")
                .when().get("/" + path + "?sort=-salary")
                .then().statusCode(400);
        given().accept("application/json")
                .auth().preemptive().basic("admin", "admin")
                .when().get("/" + path + "?sort=-salary")
                .then().statusCode(200)
                .body("name", contains("Bob", "Carol", "Alice"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "field-employees", "getter-employees" })
    void filterBySecureFieldRequiresRole(String path) {
        given().accept("application/json")
                .when().get("/" + path + "?salary=20000")
                .then().statusCode(400);
        given().accept("application/json")
                .auth().preemptive().basic("viewer", "viewer")
                .when().get("/" + path + "?salary=20000")
                .then().statusCode(400);
        given().accept("application/json")
                .auth().preemptive().basic("admin", "admin")
                .when().get("/" + path + "?salary=20000")
                .then().statusCode(200)
                .body("name", contains("Carol"));
    }

    @ResourceProperties(path = "field-employees", paged = false)
    public interface FieldEmployeesResource extends PanacheEntityResource<FieldEmployee, Long> {
    }

    @ResourceProperties(path = "getter-employees", paged = false)
    public interface GetterEmployeesResource extends PanacheEntityResource<GetterEmployee, Long> {
    }

    @Entity
    @Table(name = "field_employee")
    public static class FieldEmployee extends PanacheEntity {

        public String name;

        @SecureField(rolesAllowed = "admin")
        private Integer salary;

        public Integer getSalary() {
            return salary;
        }

        public void setSalary(Integer salary) {
            this.salary = salary;
        }
    }

    @Entity
    @Table(name = "getter_employee")
    public static class GetterEmployee extends PanacheEntity {

        public String name;

        private Integer salary;

        @SecureField(rolesAllowed = "admin")
        public Integer getSalary() {
            return salary;
        }

        public void setSalary(Integer salary) {
            this.salary = salary;
        }
    }
}
