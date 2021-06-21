package io.quarkus.amazon.sqs.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@ApplicationScoped
public class SqsClientProducer {
    private final SqsClient syncClient;
    private final SqsAsyncClient asyncClient;

    SqsClientProducer(Instance<SqsClientBuilder> syncClientBuilderInstance,
            Instance<SqsAsyncClientBuilder> asyncClientBuilderInstance) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
    }

    @Produces
    @ApplicationScoped
    public SqsClient client() {
        if (syncClient == null) {
            throw new IllegalStateException("The SqsClient is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @Produces
    @ApplicationScoped
    public SqsAsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException("The SqsAsyncClient is required but has not been detected/configured.");
        }
        return asyncClient;
    }

    @PreDestroy
    public void destroy() {
        if (syncClient != null) {
            syncClient.close();
        }
        if (asyncClient != null) {
            asyncClient.close();
        }
    }
}
