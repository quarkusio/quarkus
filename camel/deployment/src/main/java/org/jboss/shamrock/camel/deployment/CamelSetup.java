package org.jboss.shamrock.camel.deployment;

import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

public class CamelSetup implements ShamrockSetup {

    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor(new CamelProcessor());
    }

}
