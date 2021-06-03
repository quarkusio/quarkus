package io.quarkus.smallrye.graphql.deployment;

import static io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest.MEDIATYPE_JSON;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Basic tests. POST
 */
public class GraphQLTest extends AbstractGraphQLTest {

    private static final Logger LOG = Logger.getLogger(GraphQLTest.class);

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestResource.class, TestPojo.class, TestRandom.class, TestGenericsPojo.class)
                    .addAsResource(new StringAsset(getPropertyAsString(configuration())), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testSchema() {
        RequestSpecification request = RestAssured.given();
        request.accept(MEDIATYPE_TEXT);
        request.contentType(MEDIATYPE_TEXT);
        Response response = request.get("/graphql/schema.graphql");
        String body = response.body().asString();
        LOG.error(body);

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(body.contains("\"Query root\""));
        Assertions.assertTrue(body.contains("type Query {"));
        Assertions.assertTrue(body.contains("ping: TestPojo"));
        Assertions.assertTrue(body.contains("generics: TestGenericsPojo_String"));
        Assertions.assertTrue(body.contains("type TestGenericsPojo_String {"));
        Assertions.assertTrue(body.contains("enum SomeEnum {"));
        Assertions.assertTrue(body.contains("enum Number {"));
    }

    @Test
    public void testBasicPost() {
        pingTest();
    }

    @Test
    public void testBasicGet() {
        String pingRequest = "{\n" +
                "  ping {\n" +
                "    message\n" +
                "  }\n" +
                "}";

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .queryParam(QUERY, pingRequest)
                .get("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString("{\"data\":{\"ping\":{\"message\":\"pong\"}}}"));

    }

    @Test
    public void testSourcePost() {
        String fooRequest = getPayload("{\n" +
                "  foo {\n" +
                "    message\n" +
                "    randomNumber{\n" +
                "       value\n" +
                "    }\n" +
                "    list\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(fooRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString(
                        "{\"data\":{\"foo\":{\"message\":\"bar\",\"randomNumber\":{\"value\":123.0},\"list\":[\"a\",\"b\",\"c\"]}}}"));

    }

    @Test
    public void testSourcePost2() {
        String foosRequest = getPayload("{\n" +
                "  foos {\n" +
                "    message\n" +
                "    randomNumber{\n" +
                "       value\n" +
                "    }\n" +
                "    list\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(foosRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString(
                        "{\"data\":{\"foos\":[{\"message\":\"bar\",\"randomNumber\":{\"value\":123.0},\"list\":[\"a\",\"b\",\"c\"]}]}}"));

    }

    @Test
    public void testGenerics() {
        String foosRequest = getPayload("{\n" +
                "  generics {\n" +
                "    message\n" +
                "  }\n" +
                "}");

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(foosRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString(
                        "{\"data\":{\"generics\":{\"message\":\"I know it\"}}}"));
    }

    /**
     * Send a query in JSON that contains raw unescaped line breaks and tabs inside the "query" string,
     * which technically is forbidden by the JSON spec, but we want to seamlessly support
     * queries from Java text blocks, for example, which preserve line breaks and tab characters.
     */
    @Test
    public void testQueryWithNewlinesAndTabs() {
        String foosRequest = "{\"query\": \"query myquery { \n generics { \n \t message } } \"}";

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(foosRequest)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString(
                        "{\"data\":{\"generics\":{\"message\":\"I know it\"}}}"));
    }

    @Test
    public void testContext() {
        String query = getPayload("{context}");

        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString("{\"data\":{\"context\":\"/context\"}}"));
    }

    private static Map<String, String> configuration() {
        Map<String, String> m = new HashMap<>();
        m.put("quarkus.smallrye-graphql.events.enabled", "true");
        return m;
    }
}
