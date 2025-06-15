package org.jboss.resteasy.reactive.server.processor.generation.multipart;

import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.server.processor.ServerEndpointIndexer;
import org.jboss.resteasy.reactive.server.processor.generation.AbstractFeatureScanner;

public class MultipartFeature extends AbstractFeatureScanner {

    @Override
    public void integrateWithIndexer(ServerEndpointIndexer.Builder builder, IndexView index) {
        builder.setMultipartReturnTypeIndexerExtension(
                new GeneratedHandlerMultipartReturnTypeIndexerExtension(classOutput));
    }
}
