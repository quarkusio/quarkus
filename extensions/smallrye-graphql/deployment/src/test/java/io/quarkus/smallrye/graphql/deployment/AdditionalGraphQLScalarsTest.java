package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AdditionalGraphQLScalarsTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClasses(UuidApi.class)
            .addAsResource(new StringAsset("quarkus.smallrye-graphql.extra-scalars=UUID\n"
                    + "quarkus.smallrye-graphql.schema-include-scalars=true"), "application.properties")
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testUuid() {
        String uuid = UUID.randomUUID().toString();
        String query = getPayload("{ echo(uuid: \"" + uuid + "\") }");
        RestAssured.given().body(query).contentType(MEDIATYPE_JSON).post("/graphql/").then().assertThat()
                .statusCode(200).body("data.echo", equalTo(uuid));
    }

    @Test
    public void testUuidSchemaDefinition() {
        RestAssured.given().get("/graphql/schema.graphql").prettyPeek().then().assertThat()
                .body(containsString("scalar UUID")).body(containsString("echo(uuid: UUID): UUID")).statusCode(200);
    }

    @GraphQLApi
    public static class UuidApi {

        @Query
        public UUID echo(UUID uuid) {
            return uuid;
        }

    }

}
