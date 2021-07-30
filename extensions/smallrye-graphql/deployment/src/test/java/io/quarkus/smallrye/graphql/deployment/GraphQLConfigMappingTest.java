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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Basic tests. Config mapping
 */
public class GraphQLConfigMappingTest extends AbstractGraphQLTest {

    private static final Logger LOG = Logger.getLogger(GraphQLConfigMappingTest.class);

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestResource.class, TestPojo.class, TestRandom.class, TestGenericsPojo.class,
                            BusinessException.class)
                    .addAsResource(new StringAsset(getPropertyAsString(configuration())), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testBusinessError() {
        String pingRequest = "{\n" +
                "  businesserror {\n" +
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
                .body(CoreMatchers.containsString("Some invalid case"),
                        CoreMatchers.containsString("io.quarkus.smallrye.graphql.deployment.BusinessException"), // exception
                        CoreMatchers.containsString("business")); // code

    }

    @Test
    public void testSystemError() {
        String pingRequest = "{\n" +
                "  systemserror {\n" +
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
                .body(CoreMatchers.containsString("O gats, daar is 'n probleem !"), // custom message
                        CoreMatchers.containsString("java.lang.RuntimeException")); // exception

    }

    private static Map<String, String> configuration() {
        Map<String, String> m = new HashMap<>();
        m.put("quarkus.smallrye-graphql.error-extension-fields",
                "exception,classification,code,description,validationErrorType,queryPath");
        m.put("quarkus.smallrye-graphql.default-error-message", "O gats, daar is 'n probleem !");

        m.put("quarkus.smallrye-graphql.events.enabled", "true");
        return m;

    }

}
