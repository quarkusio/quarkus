package io.quarkus.elasticsearch.lowlevel.client;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.sniff.Sniffer;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ElasticsearchRestClientRecorder {

    public void configureRestClient(BeanContainer beanContainer, ElasticsearchConfig config, ShutdownContext shutdownContext) {
        RestClientBuilder builder = RestClientBuilderHelper.createRestClientBuilder(config);

        RestClient client = builder.build();
        if (config.discovery.enabled) {
            Sniffer sniffer = RestClientBuilderHelper.createSniffer(client, config);
            shutdownContext.addShutdownTask(() -> {
                try {
                    sniffer.close();
                    client.close();
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
            });
        } else {
            shutdownContext.addShutdownTask(() -> {
                try {
                    client.close();
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
            });
        }

        ElasticsearchRestClientProducer restClientProducer = beanContainer.instance(ElasticsearchRestClientProducer.class);
        restClientProducer.initialize(client);
    }
}
