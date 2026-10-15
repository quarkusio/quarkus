package io.quarkus.tck.rest;

import org.jboss.arquillian.core.spi.LoadableExtension;

public class QuarkusRestTckExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(QuarkusRestTckArchiveProcessor.class);
    }
}
