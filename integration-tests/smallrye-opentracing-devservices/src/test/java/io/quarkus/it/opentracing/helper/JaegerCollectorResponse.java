package io.quarkus.it.opentracing.helper;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.opentracing.tag.Tag;

public class JaegerCollectorResponse {

    public JaegerCollectorResponse() {
    }

    @JsonProperty("data")
    public List<Process> data;

    public List<Process> getData() {
        return data;
    }

    public static class Process {

        public Process() {
        }

        @JsonProperty
        public String traceID;

        @JsonProperty("spans")
        public List<Span> spans;

        public String getTraceID() {
            return traceID;
        }

        public List<Span> getSpans() {
            return spans;
        }

        public static class Span {

            public Span() {
            };

            @JsonProperty
            public String operationName;

            @JsonProperty
            public String traceID;

            @JsonProperty
            public String spanID;

            @JsonProperty
            public List<Tag> tags;

            public String getOperationName() {
                return operationName;
            }

            public String getTraceID() {
                return traceID;
            }

            public String getSpanID() {
                return spanID;
            }

            public List<Tag> getTags() {
                return tags;
            }

            public static class Tag {

                public Tag() {

                }

                @JsonProperty
                public String key;

                @JsonProperty
                public String value;

                public String getKey() {
                    return key;
                }

                public String getValue() {
                    return value;
                }
            }
        }
    }
}
