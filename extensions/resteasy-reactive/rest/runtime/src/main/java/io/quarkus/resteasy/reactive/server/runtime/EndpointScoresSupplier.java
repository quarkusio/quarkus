package io.quarkus.resteasy.reactive.server.runtime;

import java.util.Collections;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.server.util.ScoreSystem;

public class EndpointScoresSupplier implements Supplier<ScoreSystem.EndpointScores> {

    @Override
    public ScoreSystem.EndpointScores get() {
        var result = ScoreSystem.latestScores;
        if (result != null) {
            return result;
        }

        return new ScoreSystem.EndpointScores(0, Collections.emptyList());
    }
}
