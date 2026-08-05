package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonFormat;

public class DurationFormatBean {

    private String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Duration duration;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }
}
