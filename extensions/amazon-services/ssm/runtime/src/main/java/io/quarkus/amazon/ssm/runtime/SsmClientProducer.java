package io.quarkus.amazon.ssm.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.services.ssm.SsmAsyncClient;
import software.amazon.awssdk.services.ssm.SsmAsyncClientBuilder;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

@ApplicationScoped
public class SsmClientProducer {

    private volatile SsmClientBuilder syncConfiguredBuilder;
    private volatile SsmAsyncClientBuilder asyncConfiguredBuilder;

    private SsmClient client;
    private SsmAsyncClient asyncClient;

    @Produces
    @ApplicationScoped
    public SsmClient client() {
        client = syncConfiguredBuilder.build();
        return client;
    }

    @Produces
    @ApplicationScoped
    public SsmAsyncClient asyncClient() {
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

    public void setSyncConfiguredBuilder(SsmClientBuilder syncConfiguredBuilder) {
        this.syncConfiguredBuilder = syncConfiguredBuilder;
    }

    public void setAsyncConfiguredBuilder(SsmAsyncClientBuilder asyncConfiguredBuilder) {
        this.asyncConfiguredBuilder = asyncConfiguredBuilder;
    }
}
