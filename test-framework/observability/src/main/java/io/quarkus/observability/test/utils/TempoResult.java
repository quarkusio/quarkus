package io.quarkus.observability.test.utils;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TempoResult {
    public List<Map<Object, Object>> traces;
    public Metrics metrics;

    // getters and setters

    @Override
    public String toString() {
        return "TempoResult{" +
                "traces=" + traces +
                ", metrics=" + metrics +
                '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metrics {
        public int inspectedBytes;
        public int completedJobs;
        public int totalJobs;

        @Override
        public String toString() {
            return "Metrics{" +
                    "inspectedBytes=" + inspectedBytes +
                    ", completedJobs=" + completedJobs +
                    ", totalJobs=" + totalJobs +
                    '}';
        }
    }
}