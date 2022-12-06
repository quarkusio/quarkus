package io.quarkus.smallrye.graphql.deployment;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Basic test to verify that bean validation constraints on input fields are transformed into GraphQL directives when
 * the option to include directives in the schema is enabled.
 */
public class BeanValidationGraphQLDirectivesTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Person.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.schema-include-directives=true"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void validateDirectivesPresentInSchema() {
        get("/graphql/schema.graphql")
                .then()
                .body(containsString("input PersonInput {\n" +
                        "  name: String @constraint(maxLength : 20, minLength : 5)\n" +
                        "}\n"))
                .body(containsString(
                        "queryWithConstrainedArgument(constrained: String @constraint(maxLength : 123)): String"));
    }

    @GraphQLApi
    public static class ValidationApi {

        @Query
        public String query(@Valid Person person) {
            return null;
        }

        @Query
        public String queryWithConstrainedArgument(@Size(max = 123) String constrained) {
            return null;
        }

    }

    public static class Person {

        @Size(min = 5, max = 20)
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
