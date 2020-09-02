package io.quarkus.google.coud.service.spanner.runtime;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;

import io.quarkus.gcp.services.common.GcpConfiguration;

@ApplicationScoped
public class SpannerProducer {
    @Inject
    GoogleCredentials googleCredentials;

    @Inject
    GcpConfiguration gcpConfiguration;

    @Produces
    @Singleton
    @Default
    public Spanner storage() throws IOException {
        return SpannerOptions.newBuilder().setCredentials(googleCredentials)
                .setProjectId(gcpConfiguration.projectId.get()).build().getService();
    }
}
