package io.quarkus.funqy.knative.events;

public abstract class AbstractCloudEvent<T> implements CloudEvent<T> {
    @Override
    public String toString() {
        return "CloudEvent{" +
                "specVersion='" + specVersion() + '\'' +
                ", id='" + id() + '\'' +
                ", type='" + type() + '\'' +
                ", source='" + source() + '\'' +
                ", subject='" + subject() + '\'' +
                ", time=" + time() +
                ", extensions=" + extensions() +
                ", dataSchema=" + dataSchema() +
                ", dataContentType='" + dataContentType() + '\'' +
                ", data=" + data() +
                '}';
    }
}
