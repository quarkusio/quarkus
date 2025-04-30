package io.quarkus.funqy.lambda.event;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.mutiny.Uni;

public class EventErrorHandler {

    private final List<String> failures = new ArrayList<>();

    public Uni<?> collectFailures(Uni<?> uni, String id) {
        return uni.onTermination().invoke((item, failure, cancellation) -> {
            Throwable actualFailure = null;
            if (failure != null) {
                actualFailure = failure;
            } else if (cancellation) {
                actualFailure = new RuntimeException("Stream cancelled");
            }
            if (actualFailure != null) {
                failures.add(id);
            }
        }).onFailure().recoverWithNull();
    }

    public List<String> getFailures() {
        return failures;
    }
}
