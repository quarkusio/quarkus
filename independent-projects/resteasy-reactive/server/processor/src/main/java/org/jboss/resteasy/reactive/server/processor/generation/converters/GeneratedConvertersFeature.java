package org.jboss.resteasy.reactive.server.processor.generation.converters;

import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.server.processor.ServerEndpointIndexer;
import org.jboss.resteasy.reactive.server.processor.generation.AbstractFeatureScanner;

public class GeneratedConvertersFeature extends AbstractFeatureScanner {
    @Override
    public void integrateWithIndexer(ServerEndpointIndexer.Builder builder, IndexView index) {
        builder.setConverterSupplierIndexerExtension(new GeneratedConverterIndexerExtension((s) -> classOutput));
    }
}
