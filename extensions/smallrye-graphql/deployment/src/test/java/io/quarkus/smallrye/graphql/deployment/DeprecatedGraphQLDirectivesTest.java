package io.quarkus.smallrye.graphql.deployment;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.graphql.api.Deprecated;

public class DeprecatedGraphQLDirectivesTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Person.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-graphql.schema-include-directives=true"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void deprecatedDirectivesPresentInSchema() {
        get("/graphql/schema.graphql")
                .then()
                .body(containsString("input PersonInput {\n" +
                        "  age: Int! @deprecated\n" +
                        "  name: String @deprecated(reason : \"reason0\")\n" +
                        "  numberOfEyes: BigInteger! @deprecated\n" +
                        "}\n"))
                .body(containsString(
                        "queryWithDeprecatedArgument(deprecated: String @deprecated(reason : \"reason1\")): String"));
    }

    @GraphQLApi
    public static class ValidationApi {

        @Query
        public String query(Person person) {
            return null;
        }

        @Query
        public String queryWithDeprecatedArgument(@Deprecated(reason = "reason1") String deprecated) {
            return null;
        }

    }

    public static class Person {

        @Deprecated(reason = "reason0")
        private String name;

        @java.lang.Deprecated
        private int age;

        @Deprecated
        private long numberOfEyes;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public long getNumberOfEyes() {
            return numberOfEyes;
        }

        public void setNumberOfEyes(long numberOfEyes) {
            this.numberOfEyes = numberOfEyes;
        }
    }

}
