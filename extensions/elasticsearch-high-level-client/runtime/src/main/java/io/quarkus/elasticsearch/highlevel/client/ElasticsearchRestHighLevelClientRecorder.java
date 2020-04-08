package io.quarkus.elasticsearch.highlevel.client;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.elasticsearch.lowlevel.client.ElasticsearchConfig;
import io.quarkus.elasticsearch.lowlevel.client.RestClientBuilderHelper;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ElasticsearchRestHighLevelClientRecorder {

    public void configureRestClient(BeanContainer beanContainer, ElasticsearchConfig config, ShutdownContext shutdownContext) {
        RestClientBuilder builder = RestClientBuilderHelper.createRestClientBuilder(config);

        //FIXME sniffer if discovery is enabled ?

        RestHighLevelClient client = new RestHighLevelClient(builder);
        shutdownContext.addShutdownTask(() -> {
            try {
                client.close();
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        });

        ElasticsearchRestHighLevelClientProducer restClientProducer = beanContainer
                .instance(ElasticsearchRestHighLevelClientProducer.class);
        restClientProducer.initialize(client);
    }
}
