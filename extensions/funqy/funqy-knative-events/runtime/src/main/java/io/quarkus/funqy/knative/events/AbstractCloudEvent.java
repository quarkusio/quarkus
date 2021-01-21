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

    public static boolean isKnownSpecVersion(String ceSpecVersion) {
        return ceSpecVersion != null &&
                (ceSpecVersion.charAt(0) == '0' || ceSpecVersion.charAt(0) == '1') &&
                ceSpecVersion.charAt(1) == '.';
    }
}
