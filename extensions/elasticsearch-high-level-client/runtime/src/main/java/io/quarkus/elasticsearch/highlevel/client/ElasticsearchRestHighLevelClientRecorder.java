package io.quarkus.elasticsearch.highlevel.client;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.sniff.Sniffer;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.elasticsearch.lowlevel.client.ElasticsearchConfig;
import io.quarkus.elasticsearch.lowlevel.client.RestClientBuilderHelper;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ElasticsearchRestHighLevelClientRecorder {

    public void configureRestClient(BeanContainer beanContainer, ElasticsearchConfig config, ShutdownContext shutdownContext) {
        RestClientBuilder builder = RestClientBuilderHelper.createRestClientBuilder(config);

        RestHighLevelClient client = new RestHighLevelClient(builder);
        if (config.discovery.enabled) {
            Sniffer sniffer = RestClientBuilderHelper.createSniffer(client.getLowLevelClient(), config);
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

        ElasticsearchRestHighLevelClientProducer restClientProducer = beanContainer
                .instance(ElasticsearchRestHighLevelClientProducer.class);
        restClientProducer.initialize(client);
    }
}
