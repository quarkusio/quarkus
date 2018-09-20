package org.jboss.shamrock.weld.deployment;

import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;

public class WeldSetup implements ShamrockSetup {
    @Override
    public void setup(SetupContext context) {
        context.addResourceProcessor(new WeldAnnotationProcessor());
        context.addApplicationArchiveMarker("META-INF/beans.xml");
        context.addApplicationArchiveMarker("META-INF/services/javax.enterprise.inject.spi.Extension");
        context.addCapability(Capabilities.CDI_WELD);
    }
}
