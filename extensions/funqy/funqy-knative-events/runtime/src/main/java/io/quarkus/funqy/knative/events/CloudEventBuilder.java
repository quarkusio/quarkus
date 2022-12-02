package io.quarkus.funqy.knative.events;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;

public class CloudEventBuilder {
    private String specVersion;
    private String id;
    private String type;
    private String source;
    private String subject;
    private OffsetDateTime time;
    private String dataSchema;
    private Map<String, String> extensions;

    private CloudEventBuilder() {
    }

    public static CloudEventBuilder create() {
        return new CloudEventBuilder();
    }

    public CloudEventBuilder specVersion(String specVersion) {
        if ((specVersion.charAt(0) != '0' && specVersion.charAt(0) != '1') || specVersion.charAt(1) != '.') {
            throw new IllegalArgumentException("Only supported major versions are 0 and 1.");
        }
        this.specVersion = specVersion;
        return this;
    }

    public CloudEventBuilder id(String id) {
        this.id = id;
        return this;
    }

    public CloudEventBuilder type(String type) {
        this.type = type;
        return this;
    }

    public CloudEventBuilder source(String source) {
        this.source = source;
        return this;
    }

    public CloudEventBuilder subject(String subject) {
        this.subject = subject;
        return this;
    }

    public CloudEventBuilder time(OffsetDateTime time) {
        this.time = time;
        return this;
    }

    public CloudEventBuilder dataSchema(String dataSchema) {
        this.dataSchema = dataSchema;
        return this;
    }

    public CloudEventBuilder extensions(Map<String, String> extensions) {
        this.extensions = extensions;
        return this;
    }

    public CloudEvent<byte[]> build(byte[] data, String dataContentType) {

        return new SimpleCloudEvent(specVersion,
                id,
                type,
                source,
                subject,
                time,
                extensions,
                dataSchema,
                dataContentType,
                data);
    }

    public <T> CloudEvent<T> build(T data) {
        return new SimpleCloudEvent(specVersion,
                id,
                type,
                source,
                subject,
                time,
                extensions,
                dataSchema,
                "application/json",
                data);
    }

    public CloudEvent<Void> build() {
        return new SimpleCloudEvent(specVersion,
                id,
                type,
                source,
                subject,
                time,
                extensions,
                dataSchema,
                null,
                null);
    }

    private static final class SimpleCloudEvent<T> extends AbstractCloudEvent<T> implements CloudEvent<T> {
        private final String specVersion;
        private final String id;
        private final String type;
        private final String source;
        private final String subject;
        private final OffsetDateTime time;
        private final Map<String, String> extensions;
        private final String dataSchema;
        private final String dataContentType;
        private final T data;

        SimpleCloudEvent(String specVersion,
                String id,
                String type,
                String source,
                String subject,
                OffsetDateTime time,
                Map<String, String> extensions,
                String dataSchema,
                String dataContentType,
                T data) {

            if (extensions == null) {
                this.extensions = Collections.emptyMap();
            } else {
                this.extensions = Collections.unmodifiableMap(extensions);
            }

            this.specVersion = specVersion;
            this.id = id;
            this.type = type;
            this.source = source;
            this.subject = subject;
            this.time = time;
            this.dataSchema = dataSchema;
            this.dataContentType = dataContentType;
            this.data = data;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String specVersion() {
            return specVersion;
        }

        @Override
        public String source() {
            return source;
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public String subject() {
            return subject;
        }

        @Override
        public OffsetDateTime time() {
            return time;
        }

        @Override
        public String dataSchema() {
            return dataSchema;
        }

        @Override
        public Map<String, String> extensions() {
            return extensions;
        }

        @Override
        public String dataContentType() {
            return dataContentType;
        }

        @Override
        public T data() {
            return data;
        }

    }
}
