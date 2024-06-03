package io.quarkus.jfr.it;

public class IdResponse {
    public String traceId;
    public String spanId;

    public IdResponse() {
    }

    public IdResponse(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
    }

    @Override
    public String toString() {
        return "IdResponse{" +
                "traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                '}';
    }
}
