package io.quarkus.stackdriver.runtime.configuration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "gcp.stackdriver", phase = ConfigPhase.RUN_TIME)
public class StackdriverConfiguration {

    /**
     * The GCP project ID
     */
    @ConfigItem(name = "project-id", defaultValue = "!")
    public String projectId;

    /**
     * The GCP project ID
     */
    @ConfigItem(name = "credentials")
    public String credentials;
}
