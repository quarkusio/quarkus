package org.jboss.shamrock.beanvalidation;

import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

public class BeanValidationSetup implements ShamrockSetup {
    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor(new BeanValidationProcessor());
    }
}
