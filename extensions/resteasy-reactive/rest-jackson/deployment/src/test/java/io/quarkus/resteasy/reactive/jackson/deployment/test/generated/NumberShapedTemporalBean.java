package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

public class NumberShapedTemporalBean {

    private String name;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Instant instant;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Duration duration;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }
}
