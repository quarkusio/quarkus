package io.quarkus.arquillian;

import org.jboss.arquillian.core.spi.LoadableExtension;

public class QuarkusArquillianLifecycleExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        // for MP CP TCK only, we want to register this observer which activated CDI contexts
        builder.observer(ArquillianBeforeAfterEnricher.class);
    }
}
