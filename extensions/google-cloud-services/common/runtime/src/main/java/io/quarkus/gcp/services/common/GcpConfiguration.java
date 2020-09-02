package io.quarkus.gcp.services.common;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "google.cloud", phase = ConfigPhase.RUN_TIME)
public class GcpConfiguration {
    /**
     * Google Cloud project ID.
     */
    @ConfigItem
    public Optional<String> projectId;

    /**
     * Google Cloud service account file location.
     */
    @ConfigItem
    public Optional<String> serviceAccountLocation;
}
