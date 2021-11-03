package io.quarkus.smallrye.graphql.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * This test compatibility with the GraphQL over HTTP Spec
 */
public class GraphQLOverHttpTest extends AbstractGraphQLTest {

    private static final Logger LOG = Logger.getLogger(GraphQLOverHttpTest.class);
    private static final String MEDIATYPE_GRAPHQL = "application/graphql";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(GraphQLOverHttpApi.class, User.class)
                    .addAsResource(new StringAsset(getPropertyAsString(configuration())), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void httpGetTest() {

        RestAssured.given().when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .get("/graphql?query=query(%24id%3A%20ID!)%7Buser(id%3A%24id)%7Bname%7D%7D&variables=%7B%22id%22%3A%22QVBJcy5ndXJ1%22%7D")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString("{\"data\":{\"user\":{\"name\":\"Koos\"}}}"));
    }

    @Test
    public void httpPostWithQueryInQueryParamTest() throws Exception {

        Map<String, String> queryparams = new HashMap<>();
        queryparams.put("query", "query ($id: ID!) {  user(id:$id) {    id    name  surname}}");

        String variables = "{\"id\": \"1\"}";

        post(null, queryparams, variables,
                "{\"data\":{\"user\":{\"id\":\"1\",\"name\":\"Koos\",\"surname\":\"van der Merwe\"}}}"); // query in query parameter
    }

    @Test
    public void httpPostWithQueryInQueryParamAndBodyTest() throws Exception {

        Map<String, String> queryparams = new HashMap<>();
        queryparams.put("query", "query ($id: ID!) {  user(id:$id) {    id    name  surname}}");

        String variables = "{\"id\": \"1\"}";

        String request = "query ($id: ID!) {\n" +
                "  user(id:$id) {\n" +
                "    name\n" +
                "  }\n" +
                "}";

        post(request, queryparams, variables,
                "{\"data\":{\"user\":{\"id\":\"1\",\"name\":\"Koos\",\"surname\":\"van der Merwe\"}}}"); // query in query parameter win
    }

    @Test
    public void httpPostWithMultipleAcceptHeaders() throws Exception {

        Map<String, String> queryparams = new HashMap<>();
        queryparams.put("query", "query ($id: ID!) {  user(id:$id) {    id    name  surname}}");

        String variables = "{\"id\": \"1\"}";

        String request = "query ($id: ID!) {\n" +
                "  user(id:$id) {\n" +
                "    name\n" +
                "  }\n" +
                "}";

        post(request, queryparams, variables,
                "{\"data\":{\"user\":{\"id\":\"1\",\"name\":\"Koos\",\"surname\":\"van der Merwe\"}}}",
                List.of(MEDIATYPE_JSON, MEDIATYPE_TEXT));
    }

    @Test
    public void httpPostWithVariablesQueryParamTest() throws Exception {

        String request = "query ($id: ID!) {\n" +
                "  user(id:$id) {\n" +
                "    id\n" +
                "    name\n" +
                "  }\n" +
                "}";

        String variables = "{\"id\": \"1\"}";

        Map<String, String> queryparams = new HashMap<>();
        queryparams.put("variables", "{\"id\": \"QVBJcy5ndXJ1\"}");

        post(request, queryparams, variables, "{\"data\":{\"user\":{\"id\":\"QVBJcy5ndXJ1\",\"name\":\"Koos\"}}}"); // id in query parameter win
    }

    @Test
    public void httpPostWithContentTypeHeader() throws Exception {

        Map<String, String> queryparams = new HashMap<>();
        queryparams.put("variables", "{\"id\": \"QVBJcy5ndXJ1\"}");

        String request = "query ($id: ID!) {\n" +
                "  user(id:$id) {\n" +
                "    id\n" +
                "    name\n" +
                "    surname\n" +
                "  }\n" +
                "}";

        postAsGraphQL(request, queryparams,
                "{\"data\":{\"user\":{\"id\":\"QVBJcy5ndXJ1\",\"name\":\"Koos\",\"surname\":\"van der Merwe\"}}}");
    }

    private static Map<String, String> configuration() {
        Map<String, String> m = new HashMap<>();
        m.put("quarkus.smallrye-graphql.http.get.enabled", "true");
        m.put("quarkus.smallrye-graphql.http.post.queryparameters.enabled", "true");
        return m;
    }

    private void post(String request, Map<String, ?> queryparams, String variables, String expected) {
        post(request, queryparams, variables, expected, List.of(MEDIATYPE_JSON));
    }

    private void post(String request, Map<String, ?> queryparams, String variables, String expected,
            List<String> acceptHeaders) {
        RequestSpecification requestSpecification = RestAssured.given();
        if (queryparams != null) {
            requestSpecification = requestSpecification.queryParams(queryparams);
        }

        requestSpecification.when()
                .accept(String.join(",", acceptHeaders))
                .contentType(MEDIATYPE_JSON)
                .body(getPayload(request, variables))
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString(expected));
    }

    private void postAsGraphQL(String request, Map<String, ?> queryparams, String expected) {
        RequestSpecification requestSpecification = RestAssured.given().config(RestAssured.config()
                .encoderConfig(EncoderConfig.encoderConfig().encodeContentTypeAs(MEDIATYPE_GRAPHQL, ContentType.TEXT)))
                .contentType(MEDIATYPE_GRAPHQL);
        if (queryparams != null) {
            requestSpecification = requestSpecification.queryParams(queryparams);
        }

        requestSpecification.when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_GRAPHQL)
                .body(request) // This is the important part.
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(CoreMatchers.containsString(expected));

    }
}
