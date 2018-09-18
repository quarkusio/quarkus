package org.jboss.shamrock.jpa;

import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;
import org.jboss.shamrock.jpa.cdi.HibernateCdiResourceProcessor;

public class JpaSetup implements ShamrockSetup {
    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor(new HibernateResourceProcessor());
        context.addResourceProcessor(new HibernateCdiResourceProcessor());
    }
}
