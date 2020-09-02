package io.quarkus.gcp.services.common;

import java.io.FileInputStream;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.auth.oauth2.GoogleCredentials;

@ApplicationScoped
public class GcpCredentialProducer {

    @Inject
    GcpConfiguration gcpConfiguration;

    @Produces
    @Singleton
    @Default
    public GoogleCredentials googleCredential() throws IOException {
        if (gcpConfiguration.serviceAccountLocation.isPresent()) {
            try (FileInputStream is = new FileInputStream(gcpConfiguration.serviceAccountLocation.get())) {
                return GoogleCredentials.fromStream(is);
            }
        }

        return GoogleCredentials.getApplicationDefault();
    }
}
