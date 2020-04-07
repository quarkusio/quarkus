package io.quarkus.elasticsearch.lowlevel.client;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.elasticsearch.client.RestClient;

@ApplicationScoped
public class ElasticsearchRestClientProducer {
    private volatile RestClient client;

    void initialize(RestClient client) {
        this.client = client;
    }

    @Produces
    @Dependent
    @Default
    public RestClient restClient() {
        return this.client;
    }
}
