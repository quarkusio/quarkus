package io.quarkus.smallrye.health.runtime.produi;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Read-only health view shared by Dev UI and Prod UI. Health reporting is
 * inherently non-destructive, so all methods are safe to expose in production.
 */
@ApplicationScoped
public class HealthProdUIService {

    @Inject
    SmallRyeHealthReporter smallRyeHealthReporter;

    @JsonRpcUsage({ Usage.DEV_UI, Usage.PROD_UI })
    @JsonRpcDescription("Get the current health of the running Quarkus application")
    public Uni<SmallRyeHealth> getHealth() {
        return smallRyeHealthReporter.getHealthAsync();
    }

    @JsonRpcUsage({ Usage.DEV_UI, Usage.PROD_UI })
    @JsonRpcDescription("Stream the health of the running Quarkus application at the given interval")
    public Multi<SmallRyeHealth> streamHealth(
            @JsonRpcDescription("The refresh interval, e.g. 10s, or Off for a single reading") String interval) {
        int iv = getIntervalValue(interval);
        if (iv <= 0) {
            return smallRyeHealthReporter.getHealthAsync().toMulti();
        }
        return Multi.createFrom().ticks().every(Duration.ofSeconds(iv))
                .onItem().transformToUniAndMerge(tick -> smallRyeHealthReporter.getHealthAsync());
    }

    private int getIntervalValue(String interval) {
        if (interval == null || interval.isBlank()) {
            interval = "10s"; // default
        }
        if (interval.equalsIgnoreCase("Off")) {
            return -1;
        }
        return Integer.parseInt(interval.substring(0, interval.length() - 1));
    }
}
