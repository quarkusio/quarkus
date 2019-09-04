package io.quarkus.arquillian;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class QuarkusExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(DeployableContainer.class, QuarkusDeployableContainer.class);
        builder.service(Protocol.class, QuarkusProtocol.class);
        builder.observer(QuarkusBeforeAfterLifecycle.class);
    }

}
