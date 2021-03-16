package io.quarkus.amazon.ses.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.SesAsyncClientBuilder;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;

@ApplicationScoped
public class SesClientProducer {

    private volatile SesClientBuilder syncConfiguredBuilder;
    private volatile SesAsyncClientBuilder asyncConfiguredBuilder;

    private SesClient client;
    private SesAsyncClient asyncClient;

    @Produces
    @ApplicationScoped
    public SesClient client() {
        client = syncConfiguredBuilder.build();
        return client;
    }

    @Produces
    @ApplicationScoped
    public SesAsyncClient asyncClient() {
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

    public void setSyncConfiguredBuilder(SesClientBuilder syncConfiguredBuilder) {
        this.syncConfiguredBuilder = syncConfiguredBuilder;
    }

    public void setAsyncConfiguredBuilder(SesAsyncClientBuilder asyncConfiguredBuilder) {
        this.asyncConfiguredBuilder = asyncConfiguredBuilder;
    }
}
