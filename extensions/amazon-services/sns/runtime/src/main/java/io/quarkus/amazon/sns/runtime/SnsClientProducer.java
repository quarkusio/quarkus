package io.quarkus.amazon.sns.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

@ApplicationScoped
public class SnsClientProducer {
    private volatile SnsClientBuilder syncConfiguredBuilder;
    private volatile SnsAsyncClientBuilder asyncConfiguredBuilder;

    private SnsClient client;
    private SnsAsyncClient asyncClient;

    @Produces
    @ApplicationScoped
    public SnsClient client() {
        client = syncConfiguredBuilder.build();
        return client;
    }

    @Produces
    @ApplicationScoped
    public SnsAsyncClient asyncClient() {
        asyncClient = asyncConfiguredBuilder.build();
        return asyncClient;
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.close();
        }
        if (asyncClient != null) {
            asyncClient.close();
        }
    }

    public void setSyncConfiguredBuilder(SnsClientBuilder syncConfiguredBuilder) {
        this.syncConfiguredBuilder = syncConfiguredBuilder;
    }

    public void setAsyncConfiguredBuilder(SnsAsyncClientBuilder asyncConfiguredBuilder) {
        this.asyncConfiguredBuilder = asyncConfiguredBuilder;
    }
}
