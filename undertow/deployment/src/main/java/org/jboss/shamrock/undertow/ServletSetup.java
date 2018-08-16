package org.jboss.shamrock.undertow;

import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

public class ServletSetup implements ShamrockSetup {
    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor(new ServletResourceProcessor());
        context.addInjectionProvider(new ServletInjectionProvider());
    }
}
