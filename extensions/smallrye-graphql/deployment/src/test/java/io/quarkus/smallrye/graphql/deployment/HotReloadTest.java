package io.quarkus.smallrye.graphql.deployment;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

/**
 * Test Hot reload after a code change
 */
public class HotReloadTest extends AbstractGraphQLTest {

    private static final Logger LOG = Logger.getLogger(HotReloadTest.class);

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestResource.class, TestPojo.class, TestRandom.class, TestGenericsPojo.class,
                            BusinessException.class)
                    .addAsResource(new StringAsset(getPropertyAsString()), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

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
