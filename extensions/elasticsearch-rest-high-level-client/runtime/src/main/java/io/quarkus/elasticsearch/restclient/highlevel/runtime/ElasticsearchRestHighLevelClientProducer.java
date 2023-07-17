package io.quarkus.elasticsearch.restclient.highlevel.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

@ApplicationScoped
public class ElasticsearchRestHighLevelClientProducer {

    @Inject
    @Default
    RestClient restClient;

    private RestHighLevelClient client;

    @Produces
    @Singleton
    public RestHighLevelClient restHighLevelClient() {
        this.client = new QuarkusRestHighLevelClient(restClient, RestClient::close, Collections.emptyList());
        return this.client;
    }

    @PreDestroy
    void destroy() {
        try {
            if (this.client != null) {
                this.client.close();
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }
}
