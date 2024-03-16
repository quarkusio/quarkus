package io.quarkus.resteasy.reactive.jackson.deployment.test.streams;

import java.util.List;

public class Demands {
    public List<Long> demands;

    public Demands(List<Long> demands) {
        this.demands = demands;
    }

    // for Jsonb
    public Demands() {
    }
}
