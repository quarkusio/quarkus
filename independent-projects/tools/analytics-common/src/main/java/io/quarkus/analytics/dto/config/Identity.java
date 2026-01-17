package io.quarkus.analytics.dto.config;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

import io.quarkus.analytics.dto.segment.SegmentContext;

/**
 * Identity of the user at the upstream collection tool.
 */
public class Identity implements Serializable, SegmentContext {
    private String userId;
    private Map<String, Object> context;
    private Instant timestamp;

    public Identity(String userId, Map<String, Object> context, Instant timestamp) {
        this.userId = userId;
        this.context = context;
        this.timestamp = timestamp;
    }

    public static IdentityBuilder builder() {
        return new IdentityBuilder();
    }

    /**
     * The UUID of the user.
     *
     * @return
     */
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * The context of the user. See: AnalyticsService.createContextMap() (package friendly) for details.
     *
     * @return
     */
    @Override
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

    public static class IdentityBuilder {
        private String userId;
        private Map<String, Object> context;
        private Instant timestamp;

        IdentityBuilder() {
        }

        public IdentityBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public IdentityBuilder context(Map<String, Object> context) {
            this.context = context;
            return this;
        }

        public IdentityBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Identity build() {
            return new Identity(userId, context, timestamp);
        }

        public String toString() {
            return "Identity.IdentityBuilder(userId=" + this.userId + ", context="
                    + this.context + ", timestamp=" + this.timestamp + ")";
        }
    }
}
