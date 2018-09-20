package org.jboss.shamrock.arc.deployment;

import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

public class ArcSetup implements ShamrockSetup {

    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor(new ArcAnnotationProcessor());
        context.addCapability(Capabilities.CDI_ARC);
    }
}
