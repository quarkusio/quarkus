package io.quarkus.elasticsearch.highlevel.client;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

@ApplicationScoped
public class ElasticsearchRestHighLevelClientProducer {
    private volatile RestHighLevelClient client;

    void initialize(RestHighLevelClient client) {
        this.client = client;
    }

    @Produces
    @Dependent
    @Default
    public RestClient restClient() {
        return this.client.getLowLevelClient();
    }

    @Produces
    @Dependent
    @Default
    public RestHighLevelClient restHighLevelClient() {
        return this.client;
    }
}
