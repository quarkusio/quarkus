package io.quarkus.gcp.services.storage.runtime;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import io.quarkus.gcp.services.common.GcpConfiguration;

@ApplicationScoped
public class StorageProducer {

    @Inject
    GoogleCredentials googleCredentials;

    @Inject
    GcpConfiguration gcpConfiguration;

    @Produces
    @Singleton
    @Default
    public Storage storage() throws IOException {
        return StorageOptions.newBuilder().setCredentials(googleCredentials)
                .setProjectId(gcpConfiguration.projectId.get()).build().getService();
    }
}
