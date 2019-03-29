package io.quarkus.arc.deployment;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.deployment.test.TestScopeSetup;

public class ArcTestRequestScopeProvider implements TestScopeSetup {

    @Override
    public void setup() {
        ArcContainer container = Arc.container();
        container.requestContext().activate();

    }

    @Override
    public void tearDown() {
        ArcContainer container = Arc.container();
        container.requestContext().terminate();
    }
}
