package io.quarkus.smallrye.health.runtime.dev.ui;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

public class HealthJsonRPCService {

    @Inject
    SmallRyeHealthReporter smallRyeHealthReporter;

    private final BroadcastProcessor<SmallRyeHealth> healthStream = BroadcastProcessor.create();
    private final BroadcastProcessor<String> statusStream = BroadcastProcessor.create();
    private final AtomicReference<String> lastPayload = new AtomicReference<>("");

    @PostConstruct
    void startPolling() {
        Multi.createFrom().ticks().every(Duration.ofSeconds(3))
                .onItem().transformToUniAndMerge(tick -> smallRyeHealthReporter.getHealthAsync())
                .subscribe().with(smallRyeHealth -> {
                    String jsonStr = smallRyeHealth.getPayload().toString();
                    if (!Objects.equals(lastPayload.getAndSet(jsonStr), jsonStr)) {
                        if (smallRyeHealth != null) {
                            healthStream.onNext(smallRyeHealth);
                            statusStream.onNext(getStatusIcon(smallRyeHealth));
                        }
                    }
                }, failure -> {
                    JsonObject errorPayload = Json.createObjectBuilder()
                            .add("status", "DOWN")
                            .add("checks", Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder()
                                            .add("name", "Smallrye Health stream")
                                            .add("status", "DOWN")
                                            .add("data", Json.createObjectBuilder()
                                                    .add("reason", failure.getMessage()))))
                            .build();
                    healthStream.onNext(new SmallRyeHealth(errorPayload));
                    statusStream.onNext(DOWN_ICON);
                });
    }

    public Uni<SmallRyeHealth> getHealth() {
        return smallRyeHealthReporter.getHealthAsync();
    }

    public Multi<SmallRyeHealth> streamHealth() {
        return healthStream;
    }

    public String getStatus() {
        return getStatusIcon(smallRyeHealthReporter.getHealth());
    }

    public Multi<String> streamStatus() {
        return statusStream;
    }

    private String getStatusIcon(SmallRyeHealth smallRyeHealth) {
        if (smallRyeHealth.getPayload() != null && smallRyeHealth.getPayload().containsKey("status")) {
            String status = smallRyeHealth.getPayload().getString("status");
            if (status.equalsIgnoreCase("UP")) {
                return UP_ICON;
            }
        }
        return DOWN_ICON;
    }

    private static final String UP_ICON = "<vaadin-icon style='color:var(--lumo-success-text-color);' icon='font-awesome-solid:thumbs-up'></vaadin-icon>";
    private static final String DOWN_ICON = "<vaadin-icon style='color:var(--lumo-error-text-color);' icon='font-awesome-solid:thumbs-down'></vaadin-icon>";
}
