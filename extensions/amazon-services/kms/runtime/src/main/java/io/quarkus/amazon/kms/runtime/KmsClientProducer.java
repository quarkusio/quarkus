package io.quarkus.amazon.kms.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.KmsAsyncClientBuilder;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;

@ApplicationScoped
public class KmsClientProducer {

    private volatile KmsClientBuilder syncConfiguredBuilder;
    private volatile KmsAsyncClientBuilder asyncConfiguredBuilder;

    private KmsClient client;
    private KmsAsyncClient asyncClient;

    @Produces
    @ApplicationScoped
    public KmsClient client() {
        client = syncConfiguredBuilder.build();
        return client;
    }

    @Produces
    @ApplicationScoped
    public KmsAsyncClient asyncClient() {
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

    public void setSyncConfiguredBuilder(KmsClientBuilder syncConfiguredBuilder) {
        this.syncConfiguredBuilder = syncConfiguredBuilder;
    }

    public void setAsyncConfiguredBuilder(KmsAsyncClientBuilder asyncConfiguredBuilder) {
        this.asyncConfiguredBuilder = asyncConfiguredBuilder;
    }
}
