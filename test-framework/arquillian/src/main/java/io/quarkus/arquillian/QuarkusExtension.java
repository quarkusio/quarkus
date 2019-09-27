package io.quarkus.arquillian;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.TestEnricher;

public class QuarkusExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(DeployableContainer.class, QuarkusDeployableContainer.class);
        builder.service(Protocol.class, QuarkusProtocol.class);
        builder.service(TestEnricher.class, InjectionEnricher.class);
        builder.observer(CreationalContextDestroyer.class);
        builder.observer(QuarkusBeforeAfterLifecycle.class);
    }

}
