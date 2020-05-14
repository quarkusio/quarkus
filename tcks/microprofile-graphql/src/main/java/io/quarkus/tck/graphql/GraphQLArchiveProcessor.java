package io.quarkus.tck.graphql;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Creates the deployable unit with all the needed dependencies.
 */
public class GraphQLArchiveProcessor implements ApplicationArchiveProcessor {
    private static final Logger LOG = Logger.getLogger(GraphQLArchiveProcessor.class.getName());

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {

        if (applicationArchive instanceof WebArchive) {
            LOG.info("\n ================================================================================"
                    + "\n Testing [" + testClass.getName() + "]"
                    + "\n ================================================================================"
                    + "\n");
        }
    }
}
