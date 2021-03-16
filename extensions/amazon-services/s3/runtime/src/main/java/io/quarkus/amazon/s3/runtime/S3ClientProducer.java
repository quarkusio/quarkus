package io.quarkus.amazon.s3.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@ApplicationScoped
public class S3ClientProducer {
    private volatile S3ClientBuilder syncConfiguredBuilder;
    private volatile S3AsyncClientBuilder asyncConfiguredBuilder;

    private S3Client client;
    private S3AsyncClient asyncClient;

    @Produces
    @ApplicationScoped
    public S3Client client() {
        client = syncConfiguredBuilder.build();
        return client;
    }

    @Produces
    @ApplicationScoped
    public S3AsyncClient asyncClient() {
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

    public void setSyncConfiguredBuilder(S3ClientBuilder syncConfiguredBuilder) {
        this.syncConfiguredBuilder = syncConfiguredBuilder;
    }

    public void setAsyncConfiguredBuilder(S3AsyncClientBuilder asyncConfiguredBuilder) {
        this.asyncConfiguredBuilder = asyncConfiguredBuilder;
    }
}
