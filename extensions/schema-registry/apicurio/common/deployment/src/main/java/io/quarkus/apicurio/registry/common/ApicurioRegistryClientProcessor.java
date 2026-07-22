package io.quarkus.apicurio.registry.common;

import java.util.logging.Level;

import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.smallrye.openapi.deployment.spi.IgnoreStaticDocumentBuildItem;

public class ApicurioRegistryClientProcessor {

    private static final DotName JACKSON_DATE_TIME_CUSTOMIZER = DotName
            .createSimple("io.apicurio.registry.rest.config.JacksonDateTimeCustomizer");

    @BuildStep
    void vetoJackson2Beans(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        // Apicurio's JacksonDateTimeCustomizer implements the Jackson 2 ObjectMapperCustomizer;
        // replaced by ApicurioRegistryDateTimeCustomizer using Jackson 3
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation.forClasses()
                .whenClass(c -> c.name().equals(JACKSON_DATE_TIME_CUSTOMIZER))
                .transform(c -> c.add(jakarta.enterprise.inject.Vetoed.class))));
    }

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
