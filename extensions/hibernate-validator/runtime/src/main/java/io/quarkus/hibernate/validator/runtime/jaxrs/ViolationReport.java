package io.quarkus.hibernate.validator.runtime.jaxrs;

import java.util.List;

import jakarta.ws.rs.core.Response;

/**
 * As spec doesn't say anything about the report format,
 * we just use https://opensource.zalando.com/problem/constraint-violation
 * This also what Reactive Routes uses
 */
public class ViolationReport {
    private String title;
    private int status;
    private List<Violation> violations;

    /**
     * Requires no-args constructor for some serializers.
     */
    public ViolationReport() {
    }

    public ViolationReport(String title, Response.Status status, List<Violation> violations) {
        this.title = title;
        this.status = status.getStatusCode();
        this.violations = violations;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public void setViolations(List<Violation> violations) {
        this.violations = violations;
    }

    @Override
    public String toString() {
        return "ViolationReport{" +
                "title='" + title + '\'' +
                ", status=" + status +
                ", violations=" + violations +
                '}';
    }

    public static class Violation {
        private String field;
        private String message;

        /**
         * Requires no-args constructor for some serializers.
         */
        public Violation() {
        }

        public Violation(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "Violation{" +
                    "field='" + field + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
