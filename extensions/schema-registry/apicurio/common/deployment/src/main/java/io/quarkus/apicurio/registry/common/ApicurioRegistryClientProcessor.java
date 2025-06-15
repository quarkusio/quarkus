package io.quarkus.apicurio.registry.common;

import java.io.IOException;

import io.apicurio.rest.client.spi.ApicurioHttpClientProvider;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.smallrye.openapi.deployment.spi.IgnoreStaticDocumentBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;

public class ApicurioRegistryClientProcessor {

    @BuildStep
    public void apicurioRegistryClient(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {
        reflectiveClass
                .produce(ReflectiveClassBuildItem
                        .builder("io.apicurio.rest.client.auth.exception.NotAuthorizedException",
                                "io.apicurio.rest.client.auth.exception.ForbiddenException",
                                "io.apicurio.rest.client.auth.exception.AuthException",
                                "io.apicurio.rest.client.auth.exception.AuthErrorHandler",
                                "io.apicurio.rest.client.auth.request.TokenRequestsProvider",
                                "io.apicurio.rest.client.request.Request",
                                "io.apicurio.rest.client.auth.AccessTokenResponse", "io.apicurio.rest.client.auth.Auth",
                                "io.apicurio.rest.client.auth.BasicAuth", "io.apicurio.rest.client.auth.OidcAuth")
                        .methods().fields().build());
    }

    @BuildStep
    void registerSPIClient(BuildProducer<ServiceProviderBuildItem> services) throws IOException {

        services.produce(new ServiceProviderBuildItem(ApicurioHttpClientProvider.class.getName(),
                "io.apicurio.rest.client.VertxHttpClientProvider"));
    }

    @BuildStep
    void ignoreIncludedOpenAPIDocument(BuildProducer<IgnoreStaticDocumentBuildItem> ignoreStaticDocumentProducer) {
        // This will ignore the OpenAPI Document in META-INF/openapi.yaml in the apicurio-registry-common dependency
        ignoreStaticDocumentProducer.produce(new IgnoreStaticDocumentBuildItem(
                ".*/io/apicurio/apicurio-registry-common/.*/apicurio-registry-common-.*.jar.*"));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void apicurioRegistryClient(VertxBuildItem vertx, ApicurioRegistryClient client,
            LaunchModeBuildItem launchMode) {
        if (launchMode.getLaunchMode().isDevOrTest()) {
            client.clearHttpClient();
        }
        client.setup(vertx.getVertx());
    }

}
