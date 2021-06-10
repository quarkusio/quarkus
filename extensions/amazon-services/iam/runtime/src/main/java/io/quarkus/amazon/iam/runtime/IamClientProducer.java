package io.quarkus.amazon.iam.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.IamAsyncClientBuilder;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;

@ApplicationScoped
public class IamClientProducer {
    private final IamClient syncClient;
    private final IamAsyncClient asyncClient;

    IamClientProducer(Instance<IamClientBuilder> syncClientBuilderInstance,
            Instance<IamAsyncClientBuilder> asyncClientBuilderInstance) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
    }

    @Produces
    @ApplicationScoped
    public IamClient client() {
        if (syncClient == null) {
            throw new IllegalStateException("The IamClient is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @Produces
    @ApplicationScoped
    public IamAsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException("The IamAsyncClient is required but has not been detected/configured.");
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
