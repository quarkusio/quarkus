package org.jboss.shamrock.restclient;

import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

public class RestClientSetup implements ShamrockSetup {
    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor(new RestClientProcessor());
    }
}
