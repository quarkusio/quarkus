package io.quarkus.apicurio.registry.common;

import java.util.logging.Level;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
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

}
