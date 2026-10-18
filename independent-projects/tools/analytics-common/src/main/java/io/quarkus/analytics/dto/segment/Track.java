package io.quarkus.analytics.dto.segment;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

public class Track implements Serializable {
    private String userId;
    private TrackEventType event;
    private TrackProperties properties;
    private Map<String, Object> context;
    private Instant timestamp;

    public Track() {
    }

    public Track(String userId, TrackEventType event, TrackProperties properties, Map<String, Object> context,
            Instant timestamp) {
        this.userId = userId;
        this.event = event;
        this.properties = properties;
        this.context = context;
        this.timestamp = timestamp;
    }

    public static TrackBuilder builder() {
        return new TrackBuilder();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public TrackEventType getEvent() {
        return event;
    }

    public void setEvent(TrackEventType event) {
        this.event = event;
    }

    public TrackProperties getProperties() {
        return properties;
    }

    public void setProperties(TrackProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public static class TrackBuilder {
        private String userId;
        private TrackEventType event;
        private TrackProperties properties;
        private Map<String, Object> context;
        private Instant timestamp;

        TrackBuilder() {
        }

        public TrackBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public TrackBuilder event(TrackEventType event) {
            this.event = event;
            return this;
        }

        public TrackBuilder properties(TrackProperties properties) {
            this.properties = properties;
            return this;
        }

        public TrackBuilder context(Map<String, Object> context) {
            this.context = context;
            return this;
        }

        public TrackBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Track build() {
            return new Track(userId, event, properties, context, timestamp);
        }

        public String toString() {
            return "Track.TrackBuilder(userId=" + this.userId + ", event=" + this.event +
                    ", properties=" + this.properties + ", context=" + this.context +
                    ", timestamp=" + this.timestamp + ")";
        }
    }

    public static class EventPropertyNames {
        public static final String BUILD_DIAGNOSTICS = "build_diagnostics";
        public static final String APP_EXTENSIONS = "app_extensions";
    }
}
