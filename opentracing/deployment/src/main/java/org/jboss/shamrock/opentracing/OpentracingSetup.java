package org.jboss.shamrock.opentracing;

import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

public class OpentracingSetup implements ShamrockSetup  {
    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor( new OpentracingProcessor() );

    }
}
