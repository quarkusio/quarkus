package io.quarkus.tck.jwt;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class JwtTckExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(ApplicationArchiveProcessor.class, JwtTckArchiveProcessor.class);
    }
}
