package io.quarkus.smallrye.health.runtime.dev.ui;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class HealthJsonRPCService {

    @Inject
    SmallRyeHealthReporter smallRyeHealthReporter;

    private final Set<MultiEmitter<? super SmallRyeHealth>> healthEmitters = ConcurrentHashMap.newKeySet();
    private final Set<MultiEmitter<? super String>> statusEmitters = ConcurrentHashMap.newKeySet();

    private final AtomicInteger activeSubscribers = new AtomicInteger(0);
    private final AtomicReference<SmallRyeHealth> latest = new AtomicReference<>();
    private volatile Cancellable pollingCancellable;

    private synchronized void startPollingIfNeeded(int interval) {
        if (pollingCancellable == null) {
            pollingCancellable = Multi.createFrom().ticks().every(Duration.ofSeconds(interval))
                    .onItem().transformToUniAndMerge(tick -> smallRyeHealthReporter.getHealthAsync())
                    .subscribe().with(smallRyeHealth -> {
                        latest.set(smallRyeHealth);
                        for (var emitter : healthEmitters) {
                            emitter.emit(smallRyeHealth);
                        }
                        for (var emitter : statusEmitters) {
                            emitter.emit(getStatusIcon(smallRyeHealth));
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
                        SmallRyeHealth errorHealth = new SmallRyeHealth(errorPayload);
                        latest.set(errorHealth);
                        for (var emitter : healthEmitters) {
                            emitter.emit(errorHealth);
                        }
                        for (var emitter : statusEmitters) {
                            emitter.emit(getStatusIcon(errorHealth));
                        }
                    });
        }
    }

    private synchronized void stopPolling() {
        if (pollingCancellable != null) {
            pollingCancellable.cancel();
            pollingCancellable = null;
            latest.set(null);
        }
    }

    private synchronized void restartPolling(int interval) {
        stopPolling();
        if (interval > 0) {
            startPollingIfNeeded(interval);
        }
    }

    public Uni<SmallRyeHealth> getHealth() {
        return smallRyeHealthReporter.getHealthAsync();
    }

    public Multi<SmallRyeHealth> streamHealth(String interval) {
        int iv = getIntervalValue(interval);

        return Multi.createFrom().emitter(emitter -> {
            activeSubscribers.incrementAndGet();
            healthEmitters.add(emitter);

            SmallRyeHealth current = latest.get();
            if (current != null) {
                emitter.emit(current);
            }

            restartPolling(iv);

            emitter.onTermination(() -> {
                healthEmitters.remove(emitter);
                if (activeSubscribers.decrementAndGet() == 0) {
                    stopPolling();
                }
            });
        }, BackPressureStrategy.LATEST);
    }

    public String getStatus() {
        return getStatusIcon(smallRyeHealthReporter.getHealth());
    }

    public Multi<String> streamStatus(String interval) {
        int iv = getIntervalValue(interval);

        return Multi.createFrom().emitter(emitter -> {
            activeSubscribers.incrementAndGet();
            statusEmitters.add(emitter);

            SmallRyeHealth current = latest.get();
            if (current != null) {
                emitter.emit(getStatusIcon(current));
            }

            restartPolling(iv);

            emitter.onTermination(() -> {
                statusEmitters.remove(emitter);
                if (activeSubscribers.decrementAndGet() == 0) {
                    stopPolling();
                }
            });
        }, BackPressureStrategy.LATEST);
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

    private int getIntervalValue(String interval) {
        if (interval == null || interval.isBlank()) {
            interval = "10s"; //default
        }
        if (interval.equalsIgnoreCase("Off")) {
            return -1;
        }

        return Integer.parseInt(interval.substring(0, interval.length() - 1));
    }

    private static final String UP_ICON = "<vaadin-icon style='color:var(--lumo-success-text-color);' icon='font-awesome-solid:thumbs-up'></vaadin-icon>";
    private static final String DOWN_ICON = "<vaadin-icon style='color:var(--lumo-error-text-color);' icon='font-awesome-solid:thumbs-down'></vaadin-icon>";
}
