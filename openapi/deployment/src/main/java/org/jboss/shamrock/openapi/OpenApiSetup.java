package org.jboss.shamrock.openapi;

import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

/**
 * @author Ken Finnigan
 */
public class OpenApiSetup implements ShamrockSetup {
    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor(new OpenApiProcessor());
    }
}
