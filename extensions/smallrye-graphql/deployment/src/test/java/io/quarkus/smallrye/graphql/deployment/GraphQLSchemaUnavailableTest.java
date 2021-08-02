package io.quarkus.smallrye.graphql.deployment;

import java.util.HashMap;
import java.util.Map;

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
 * Test when schema is not available
 */
public class GraphQLSchemaUnavailableTest extends AbstractGraphQLTest {

    private static final Logger LOG = Logger.getLogger(GraphQLSchemaUnavailableTest.class);

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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

        Assertions.assertEquals(404, response.statusCode());
    }

    private static Map<String, String> configuration() {
        Map<String, String> m = new HashMap<>();
        m.put("quarkus.smallrye-graphql.schema-available", "false");
        return m;
    }
}
