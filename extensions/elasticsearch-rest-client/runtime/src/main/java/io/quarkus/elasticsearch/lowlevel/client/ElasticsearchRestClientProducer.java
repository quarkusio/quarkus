package io.quarkus.elasticsearch.lowlevel.client;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.sniff.Sniffer;

@ApplicationScoped
public class ElasticsearchRestClientProducer {
    @Inject
    ElasticsearchConfig config;

    private RestClient client;
    private Sniffer sniffer;

    @PostConstruct
    void initialize() {
        RestClientBuilder builder = RestClientBuilderHelper.createRestClientBuilder(config);

        this.client = builder.build();
        if (config.discovery.enabled) {
            this.sniffer = RestClientBuilderHelper.createSniffer(client, config);
        }
    }

    @PreDestroy
    void destroy() {
        try {
            if (this.sniffer != null) {
                this.sniffer.close();
            }
            this.client.close();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    @Produces
    @Singleton
    public RestClient restClient() {
        return this.client;
    }
}
