package io.quarkus.security.webauthn.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ExcludeConfigBuildItem;

public class JansiOverrideMetadata {

    static final String JANSI_JAR_MATCH_REGEX = "org\\.fusesource\\.jansi\\.jansi";
    static final String JANSI_JNI_CONFIG_MATCH_REGEX = "META-INF/native-image/jansi/jni-config\\.json";
    static final String JANSI_RESOURCE_CONFIG_MATCH_REGEX = "META-INF/native-image/jansi/resource-config\\.json";

    @BuildStep
    void excludeJansiDirectives(BuildProducer<ExcludeConfigBuildItem> nativeImageExclusions) {
        nativeImageExclusions
                .produce(new ExcludeConfigBuildItem(JANSI_JAR_MATCH_REGEX, JANSI_JNI_CONFIG_MATCH_REGEX));
        nativeImageExclusions
                .produce(new ExcludeConfigBuildItem(JANSI_JAR_MATCH_REGEX, JANSI_RESOURCE_CONFIG_MATCH_REGEX));
    }
}
