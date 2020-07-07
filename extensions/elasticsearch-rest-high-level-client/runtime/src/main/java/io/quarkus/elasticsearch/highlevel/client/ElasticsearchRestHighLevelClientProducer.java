package io.quarkus.elasticsearch.highlevel.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

@ApplicationScoped
public class ElasticsearchRestHighLevelClientProducer {
    @Inject
    RestClient restClient;

    private RestHighLevelClient client;

    @PostConstruct
    void initialize() {
        this.client = new QuarkusRestHighLevelClient(restClient, RestClient::close, Collections.emptyList());
    }

    @PreDestroy
    void destroy() {
        try {
            this.client.close();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    @Produces
    @Singleton
    public RestHighLevelClient restHighLevelClient() {
        return this.client;
    }
}
