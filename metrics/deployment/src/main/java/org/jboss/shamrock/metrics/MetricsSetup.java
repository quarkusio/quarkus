package org.jboss.shamrock.metrics;

import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

public class MetricsSetup implements ShamrockSetup{
    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor(new MetricsProcessor());
    }
}
