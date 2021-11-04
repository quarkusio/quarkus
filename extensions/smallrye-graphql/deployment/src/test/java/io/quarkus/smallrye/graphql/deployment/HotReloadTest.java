package io.quarkus.smallrye.graphql.deployment;

import static io.quarkus.smallrye.graphql.deployment.AbstractGraphQLTest.MEDIATYPE_JSON;

import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Test Hot reload after a code change
 */
public class HotReloadTest extends AbstractGraphQLTest {

    private static final Logger LOG = Logger.getLogger(HotReloadTest.class);

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class, TestPojo.class, TestRandom.class, TestGenericsPojo.class,
                            BusinessException.class)
                    .addAsResource(new StringAsset(getPropertyAsString()), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testAddAndRemoveFieldChange() {

        String fooRequest = getPayload("{\n" +
                "  foo {\n" +
                "    message\n" +
                "    randomNumber{\n" +
                "       value\n" +
                "    }\n" +
                "    foo\n" +
                "    list\n" +
                "  }\n" +
                "}");

        // Do a request 
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
                        "{\"errors\":[{\"message\":\"Validation error of type FieldUndefined: Field 'foo' in type 'TestPojo' is undefined @ 'foo/foo'\",\"locations\":[{\"line\":7,\"column\":5}],\"extensions\":{\"classification\":\"ValidationError\"}}],\"data\":null}"));
        LOG.info("Initial request done");

        // Make a code change (add a field)
        TEST.modifySourceFile("TestPojo.java", s -> s.replace("// <placeholder>",
                "private String foo = \"bar\";\n" +
                        "    public String getFoo(){\n" +
                        "        return foo;\n" +
                        "    }"));
        LOG.info("Code change done - field added");

        // Do the request again
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
                        "{\"data\":{\"foo\":{\"message\":\"bar\",\"randomNumber\":{\"value\":123.0},\"foo\":\"bar\",\"list\":[\"a\",\"b\",\"c\"]}}}"));
        LOG.info("Hot reload done");

        // Make a code change again (remove)
        TEST.modifySourceFile("TestPojo.java", s -> s.replace("private String foo = \"bar\";\n" +
                "    public String getFoo(){\n" +
                "        return foo;\n" +
                "    }", "// <placeholder>"));

        // Do the request yet again
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
                        "{\"errors\":[{\"message\":\"Validation error of type FieldUndefined: Field 'foo' in type 'TestPojo' is undefined @ 'foo/foo'\",\"locations\":[{\"line\":7,\"column\":5}],\"extensions\":{\"classification\":\"ValidationError\"}}],\"data\":null}"));

        LOG.info("Code change done - field removed");

    }

    @Test
    public void testCodeChange() {
        // Do a request 
        pingTest();
        LOG.info("Initial ping done");

        // Make a code change
        TEST.modifySourceFile("TestResource.java", s -> s.replace("// <placeholder>",
                "    @Query(\"pong\")\n" +
                        "    public TestPojo pong() {\n" +
                        "        return new TestPojo(\"ping\");\n" +
                        "    }"));
        LOG.info("Code change done");

        // Do a request again
        pongTest();
        LOG.info("Pong done");
    }

}
