package org.jboss.shamrock.faulttolerance.deployment;

import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

public class FaultToleranceSetup implements ShamrockSetup {

    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor(new FaultToleranceAnnotationProcessor());
    }
}
