package io.quarkus.elasticsearch.javaclient.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.elasticsearch.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

@ApplicationScoped
public class ElasticsearchJavaClientProducer {

    @Inject
    @Default
    RestClient restClient;

    @Inject
    ObjectMapper objectMapper;

    private ElasticsearchClient client;
    private ElasticsearchAsyncClient asyncClient;
    private ElasticsearchTransport transport;

    @PostConstruct
    void initTransport() {
        this.transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    }

    @Produces
    @Singleton
    public ElasticsearchClient blockingClient() {
        this.client = new ElasticsearchClient(this.transport);
        return this.client;
    }

    @Produces
    @Singleton
    public ElasticsearchAsyncClient asyncClient() {
        this.asyncClient = new ElasticsearchAsyncClient(this.transport);
        return this.asyncClient;
    }

    @PreDestroy
    void destroy() {
        try {
            if (this.transport != null) {
                this.transport.close();
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

}
