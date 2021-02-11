package io.quarkus.resteasy.reactive.server.runtime;

import java.util.function.Supplier;

import org.jboss.resteasy.reactive.server.util.ScoreSystem;

public class EndpointScoresSupplier implements Supplier<ScoreSystem.EndpointScores> {

    @Override
    public ScoreSystem.EndpointScores get() {
        return ScoreSystem.latestScores;
    }
}
