package io.quarkus.apicurio.registry.common;

import java.util.logging.Level;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.smallrye.openapi.deployment.spi.IgnoreStaticDocumentBuildItem;

public class ApicurioRegistryClientProcessor {

    @BuildStep
    void ignoreIncludedOpenAPIDocument(BuildProducer<IgnoreStaticDocumentBuildItem> ignoreStaticDocumentProducer) {
        // This will ignore the OpenAPI Document in META-INF/openapi.yaml in the apicurio-registry-common dependency
        ignoreStaticDocumentProducer.produce(new IgnoreStaticDocumentBuildItem(
                ".*/io/apicurio/apicurio-registry-common/.*/apicurio-registry-common-.*.jar.*"));
    }

    @BuildStep
    void logging(BuildProducer<LogCategoryBuildItem> log) {
        // Reduce the log level of Apicurio Registry client to avoid verbose INFO messages
        // See https://github.com/quarkusio/quarkus/issues/51008
        log.produce(new LogCategoryBuildItem("io.apicurio.registry.client", Level.WARNING));
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        // Initialize RegistryClientRequestAdapterFactory at runtime because it contains inner classes
        // (JdkAuthenticatedRequestAdapter, JdkOAuth2RequestAdapter) that extend JDKRequestAdapter
        // from kiota-http-jdk, which is excluded from the classpath. Runtime initialization prevents
        // GraalVM from trying to analyze these classes at build time.
        runtimeInitializedClass
                .produce(new RuntimeInitializedClassBuildItem(
                        "io.apicurio.registry.client.common.RegistryClientRequestAdapterFactory"));
    }

}
