package io.quarkus.amazon.secretsmanager.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClientBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

@ApplicationScoped
public class SecretsManagerClientProducer {
    private final SecretsManagerClient syncClient;
    private final SecretsManagerAsyncClient asyncClient;

    SecretsManagerClientProducer(Instance<SecretsManagerClientBuilder> syncClientBuilderInstance,
            Instance<SecretsManagerAsyncClientBuilder> asyncClientBuilderInstance) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
    }

    @Produces
    @ApplicationScoped
    public SecretsManagerClient client() {
        if (syncClient == null) {
            throw new IllegalStateException("The SecretsManagerClient is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @Produces
    @ApplicationScoped
    public SecretsManagerAsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException("The SecretsManagerAsyncClient is required but has not been detected/configured.");
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
