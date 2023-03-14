package io.quarkus.arc.arquillian;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.core.spi.LoadableExtension;

import io.quarkus.arc.arquillian.utils.Hacks;

public class ArcExtension implements LoadableExtension {
    // this is called early enough
    static {
        Hacks.preventFileHandleLeaks();
    }

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(DeployableContainer.class, ArcDeployableContainer.class);
        builder.service(Protocol.class, ArcProtocol.class);
    }
}
