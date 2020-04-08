package io.quarkus.elasticsearch.lowlevel.client;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ElasticsearchRestClientRecorder {

    public void configureRestClient(BeanContainer beanContainer, ElasticsearchConfig config, ShutdownContext shutdownContext) {
        RestClientBuilder builder = RestClientBuilderHelper.createRestClientBuilder(config);

        //FIXME sniffer if discovery is enabled ?

        RestClient client = builder.build();
        shutdownContext.addShutdownTask(() -> {
            try {
                client.close();
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        });

        ElasticsearchRestClientProducer restClientProducer = beanContainer.instance(ElasticsearchRestClientProducer.class);
        restClientProducer.initialize(client);
    }
}
