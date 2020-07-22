package io.quarkus.elasticsearch.restclient.highlevel.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

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
