package io.quarkus.arc.deployment;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.deployment.test.TestScopeSetup;

public class ArcTestRequestScopeProvider implements TestScopeSetup {

    private static final Logger LOGGER = Logger.getLogger(ArcTestRequestScopeProvider.class);

    @Override
    public void setup() {
        ArcContainer container = Arc.container();
        if (container == null) {
            LOGGER.warn("ArcTestRequestScopeProvider: container is null, ignoring setup");
        } else {
            container.requestContext().activate();
        }

    }

    @Override
    public void tearDown() {
        ArcContainer container = Arc.container();
        if (container == null) {
            LOGGER.warn("ArcTestRequestScopeProvider: container is null, ignoring tearDown");
        } else {
            container.requestContext().terminate();
        }
    }
}
