package io.quarkus.smallrye.graphql.deployment;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that the GraphQL extension does not activate OpenTelemetry capability within SmallRye GraphQL
 * when there is no tracer enabled.
 */
public class GraphQLTracingDisabledTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class, TestPojo.class, TestRandom.class, TestGenericsPojo.class,
                            BusinessException.class, TestUnion.class, TestUnionMember.class)
                    .addAsResource(new StringAsset("quarkus.jaeger.enabled=false"), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testTracing() {
        // just check that querying works
        pingTest();
    }

}
