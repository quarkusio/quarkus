package io.quarkus.google.cloud.bigquery.runtime;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;

import io.quarkus.gcp.services.common.GcpConfiguration;

@ApplicationScoped
public class BigQueryProducer {

    @Inject
    GoogleCredentials googleCredentials;

    @Inject
    GcpConfiguration gcpConfiguration;

    @Produces
    @Singleton
    @Default
    public BigQuery bigQuery() throws IOException {
        return BigQueryOptions.newBuilder().setCredentials(googleCredentials)
                .setProjectId(gcpConfiguration.projectId.get()).build().getService();
    }
}
