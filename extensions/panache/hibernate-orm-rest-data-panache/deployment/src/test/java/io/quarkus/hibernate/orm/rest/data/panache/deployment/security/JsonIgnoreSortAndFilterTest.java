package io.quarkus.hibernate.orm.rest.data.panache.deployment.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.test.QuarkusExtensionTest;

class JsonIgnoreSortAndFilterTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FieldEmployee.class, GetterEmployee.class, FieldEmployeesResource.class,
                            GetterEmployeesResource.class, WriteOnlyFieldEmployee.class, WriteOnlyGetterEmployee.class,
                            WriteOnlyFieldEmployeesResource.class, WriteOnlyGetterEmployeesResource.class)
                    .addAsResource(new StringAsset(
                            """
                                    insert into field_employee(id, name, salary) values (1, 'Alice', 10000);
                                    insert into getter_employee(id, name, salary) values (1, 'Alice', 10000);
                                    insert into write_only_field_employee(id, name, salary) values (1, 'Alice', 10000);
                                    insert into write_only_getter_employee(id, name, salary) values (1, 'Alice', 10000);
                                    """),
                            "import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-h2-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-rest-jackson-deployment", Version.getVersion())));

    @ParameterizedTest
    @ValueSource(strings = { "field-employees", "getter-employees", "write-only-field-employees",
            "write-only-getter-employees" })
    void sortByIgnoredFieldIsRejected(String path) {
        given().accept("application/json")
                .when().get("/" + path + "?sort=-salary")
                .then().statusCode(400)
                .body(equalTo("Unknown sort field 'salary'"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "field-employees", "getter-employees", "write-only-field-employees",
            "write-only-getter-employees" })
    void filterByIgnoredFieldIsRejected(String path) {
        given().accept("application/json")
                .when().get("/" + path + "?salary=10000")
                .then().statusCode(400)
                .body(equalTo("Unknown filter field 'salary'"));
    }

    @ResourceProperties(path = "field-employees", paged = false)
    public interface FieldEmployeesResource extends PanacheEntityResource<FieldEmployee, Long> {
    }

    @ResourceProperties(path = "getter-employees", paged = false)
    public interface GetterEmployeesResource extends PanacheEntityResource<GetterEmployee, Long> {
    }

    @ResourceProperties(path = "write-only-field-employees", paged = false)
    public interface WriteOnlyFieldEmployeesResource extends PanacheEntityResource<WriteOnlyFieldEmployee, Long> {
    }

    @ResourceProperties(path = "write-only-getter-employees", paged = false)
    public interface WriteOnlyGetterEmployeesResource extends PanacheEntityResource<WriteOnlyGetterEmployee, Long> {
    }

    @Entity
    @Table(name = "field_employee")
    public static class FieldEmployee extends PanacheEntity {

        public String name;

        @JsonIgnore
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

        @JsonIgnore
        public Integer getSalary() {
            return salary;
        }

        public void setSalary(Integer salary) {
            this.salary = salary;
        }
    }

    @Entity
    @Table(name = "write_only_field_employee")
    public static class WriteOnlyFieldEmployee extends PanacheEntity {

        public String name;

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private Integer salary;

        public Integer getSalary() {
            return salary;
        }

        public void setSalary(Integer salary) {
            this.salary = salary;
        }
    }

    @Entity
    @Table(name = "write_only_getter_employee")
    public static class WriteOnlyGetterEmployee extends PanacheEntity {

        public String name;

        private Integer salary;

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        public Integer getSalary() {
            return salary;
        }

        public void setSalary(Integer salary) {
            this.salary = salary;
        }
    }
}
