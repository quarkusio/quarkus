package io.quarkus.google.cloud.functions.test;

/**
 * Type of the function to be launched for your test.
 */
public enum FunctionType {
    /** A function that implements <code>com.google.cloud.functions.HttpFunction</code>. **/
    HTTP("io.quarkus.gcp.functions.QuarkusHttpFunction", "http", "quarkus.google-cloud-functions.function"),

    /** A function that implements <code>com.google.cloud.functions.BackgroundFunction</code>. **/
    BACKGROUND("io.quarkus.gcp.functions.QuarkusBackgroundFunction", "event",
            "quarkus.google-cloud-functions.function"),

    /** A function that implements <code>com.google.cloud.functions.RawBackgroundFunction</code>. **/
    RAW_BACKGROUND("io.quarkus.gcp.functions.QuarkusBackgroundFunction", "event",
            "quarkus.google-cloud-functions.function"),

    /** A function that implements <code>com.google.cloud.functions.CloudEventsFunction</code>. **/
    CLOUD_EVENTS("io.quarkus.gcp.functions.QuarkusCloudEventsFunction", "cloudevent",
            "quarkus.google-cloud-functions.function"),

    /** A Funqy background function. **/
    FUNQY_BACKGROUND("io.quarkus.funqy.gcp.functions.FunqyBackgroundFunction", "event", "quarkus.funqy.export"),

    /** A Funqy cloud events function. **/
    FUNQY_CLOUD_EVENTS("io.quarkus.funqy.gcp.functions.FunqyCloudEventsFunction", "cloudevent", "quarkus.funqy.export");

    private final String target;
    private final String signatureType;
    private final String functionProperty;

    FunctionType(String target, String signatureType, String functionProperty) {
        this.target = target;
        this.signatureType = signatureType;
        this.functionProperty = functionProperty;
    }

    public String getTarget() {
        return target;
    }

    public String getSignatureType() {
        return signatureType;
    }

    public String getFunctionProperty() {
        return functionProperty;
    }
}
