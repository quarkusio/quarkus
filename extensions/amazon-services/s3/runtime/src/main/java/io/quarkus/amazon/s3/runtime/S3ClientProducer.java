package io.quarkus.amazon.s3.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@ApplicationScoped
public class S3ClientProducer {
    private final S3Client syncClient;
    private final S3AsyncClient asyncClient;

    S3ClientProducer(Instance<S3ClientBuilder> syncClientBuilderInstance,
            Instance<S3AsyncClientBuilder> asyncClientBuilderInstance) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
    }

    @Produces
    @ApplicationScoped
    public S3Client client() {
        if (syncClient == null) {
            throw new IllegalStateException("The S3Client is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @Produces
    @ApplicationScoped
    public S3AsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException("The S3AsyncClient is required but has not been detected/configured.");
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
