package io.quarkus.smallrye.graphql.deployment;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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
public class GraphQLNamingTest extends AbstractGraphQLTest {

    private static final Logger LOG = Logger.getLogger(GraphQLNamingTest.class);

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class, TestPojo.class, TestRandom.class, TestGenericsPojo.class,
                            BusinessException.class)
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
        Assertions.assertTrue(body.contains("enum TestPojoNumber {")); // This is the important part. TestPojo merged with Number
    }

    private static Map<String, String> configuration() {
        Map<String, String> m = new HashMap<>();
        m.put("quarkus.smallrye-graphql.auto-name-strategy", "MergeInnerClass");
        m.put("quarkus.smallrye-graphql.events.enabled", "true");
        return m;

    }

}
