package io.quarkus.smallrye.graphql.deployment;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class, TestPojo.class, TestRandom.class, TestGenericsPojo.class,
                            TestUnion.class, TestUnionMember.class, CustomDirective.class, BusinessException.class)
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
        Assertions.assertTrue(body.contains("type TestPojo @customDirective(fields : [\"test-pojo\"])"));
        Assertions.assertTrue(body.contains("message: String @customDirective(fields : [\"message\"])"));
        Assertions.assertTrue(body.contains("union TestUnion = TestUnionMember"));
        Assertions.assertTrue(body.contains("testUnion: TestUnion"));
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

    @ParameterizedTest
    @ValueSource(strings = { MEDIATYPE_JSON, MEDIATYPE_MULTIPART })
    public void testSourcePost(String contentType) {
        String fooRequest = getPayload("{\n" +
                "  foo {\n" +
                "    message\n" +
                "    randomNumber{\n" +
                "       value\n" +
                "    }\n" +
                "    list\n" +
                "  }\n" +
                "}");

        post("/graphql", contentType, fooRequest)
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString(
                        "{\"data\":{\"foo\":{\"message\":\"bar\",\"randomNumber\":{\"value\":123.0},\"list\":[\"a\",\"b\",\"c\"]}}}"));

    }

    @ParameterizedTest
    @ValueSource(strings = { MEDIATYPE_JSON, MEDIATYPE_MULTIPART })
    public void testWrongAcceptType(String contentType) {
        String fooRequest = getPayload("{\n" +
                "  foo {\n" +
                "    message\n" +
                "    randomNumber{\n" +
                "       value\n" +
                "    }\n" +
                "    list\n" +
                "  }\n" +
                "}");

        post("/graphql", contentType, fooRequest, MEDIATYPE_TEXT)
                .then()
                .assertThat()
                .statusCode(406);
    }

    @ParameterizedTest
    @ValueSource(strings = { MEDIATYPE_JSON, MEDIATYPE_MULTIPART })
    public void testUTF8Charset(String contentType) {
        String fooRequest = getPayload("{\n" +
                "  testCharset(characters:\"óôöúüýáâäçéëíî®©\")\n" +
                "}");

        byte[] response = post("/graphql", contentType, fooRequest)
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .extract().body().asByteArray();

        String decodedResponse = new String(response, Charset.forName("UTF-8"));
        Assertions.assertTrue(decodedResponse.contains("{\"data\":{\"testCharset\":\"óôöúüýáâäçéëíî®©\"}}"));
    }

    @ParameterizedTest
    @ValueSource(strings = { MEDIATYPE_JSON, MEDIATYPE_MULTIPART })
    public void testCP1250Charset(String contentType) {
        String fooRequest = getPayload("{\n" +
                "  testCharset(characters:\"óôöúüýáâäçéëíî®©\")\n" +
                "}");

        byte[] response = post("/graphql", contentType, fooRequest, "application/json;charset=CP1250")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .extract().body().asByteArray();

        String decodedResponse = new String(response, Charset.forName("CP1250"));
        Assertions.assertTrue(decodedResponse.contains("{\"data\":{\"testCharset\":\"óôöúüýáâäçéëíî®©\"}}"));
    }

    @ParameterizedTest
    @ValueSource(strings = { MEDIATYPE_JSON, MEDIATYPE_MULTIPART })
    public void testSourcePost2(String contentType) {
        String foosRequest = getPayload("{\n" +
                "  foos {\n" +
                "    message\n" +
                "    randomNumber{\n" +
                "       value\n" +
                "    }\n" +
                "    list\n" +
                "  }\n" +
                "}");

        post("/graphql", contentType, foosRequest)
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString(
                        "{\"data\":{\"foos\":[{\"message\":\"bar\",\"randomNumber\":{\"value\":123.0},\"list\":[\"a\",\"b\",\"c\"]}]}}"));

    }

    @ParameterizedTest
    @ValueSource(strings = { MEDIATYPE_JSON, MEDIATYPE_MULTIPART })
    public void testGenerics(String contentType) {
        String foosRequest = getPayload("{\n" +
                "  generics {\n" +
                "    message\n" +
                "  }\n" +
                "}");

        post("/graphql", contentType, foosRequest)
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
    @ParameterizedTest
    @ValueSource(strings = { MEDIATYPE_JSON, MEDIATYPE_MULTIPART })
    public void testQueryWithNewlinesAndTabs(String contentType) {
        String foosRequest = "{\"query\": \"query myquery { \n generics { \n \t message } } \"}";

        post("/graphql", contentType, foosRequest)
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString(
                        "{\"data\":{\"generics\":{\"message\":\"I know it\"}}}"));
    }

    @ParameterizedTest
    @ValueSource(strings = { MEDIATYPE_JSON, MEDIATYPE_MULTIPART })
    public void testContext(String contentType) {
        String query = getPayload("{context}");

        post("/graphql", contentType, query)
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString("{\"data\":{\"context\":\"/context\"}}"));
    }

    @ParameterizedTest
    @ValueSource(strings = { MEDIATYPE_JSON, MEDIATYPE_MULTIPART })
    public void testUnion(String contentType) {
        String unionRequest = getPayload("{\n" +
                "  testUnion {\n" +
                "    __typename\n" +
                "    ... on TestUnionMember {\n" +
                "      name\n" +
                "    }\n" +
                "  }\n" +
                "}");

        post("/graphql", contentType, unionRequest)
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString(
                        "{\"data\":{\"testUnion\":{\"__typename\":\"TestUnionMember\",\"name\":\"what is my name\"}}}"));
    }

    private static Map<String, String> configuration() {
        Map<String, String> m = new HashMap<>();
        m.put("quarkus.smallrye-graphql.events.enabled", "true");
        m.put("quarkus.smallrye-graphql.schema-include-directives", "true");
        return m;
    }
}
