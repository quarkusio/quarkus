package io.quarkus.gcp.common.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gcp.services.common.GcpCredentialProducer;

public class CommonBuildSteps {

    private static final String[] REGISTER_FOR_REFLECTION = {
            "com.google.api.client.json.GenericJson",
            "com.google.api.client.googleapis.json.GoogleJsonError",
            "com.google.api.client.googleapis.json.GoogleJsonError$ErrorInfo",
            "com.google.api.client.util.GenericData",
            "com.google.api.client.json.webtoken.JsonWebSignature",
            "com.google.api.client.json.webtoken.JsonWebToken$Payload",
            "com.google.api.client.json.webtoken.JsonWebToken$Header",
            "com.google.api.client.json.webtoken.JsonWebSignature$Header",
            "com.google.api.client.json.webtoken.JsonWebToken$Payload"
    };

    @BuildStep
    public ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(true, true, REGISTER_FOR_REFLECTION);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(GcpCredentialProducer.class);
    }

    @BuildStep
    public ExtensionSslNativeSupportBuildItem ssl() {
        return new ExtensionSslNativeSupportBuildItem("google-cloud-common");
    }
}
