package io.quarkus.tls;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.runtime.CertificateRecorder;
import io.quarkus.tls.runtime.LetsEncryptRecorder;
import io.quarkus.tls.runtime.config.TlsConfig;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.quarkus.vertx.http.deployment.spi.RouteBuildItem;

public class CertificatesProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public TlsRegistryBuildItem initializeCertificate(
            TlsConfig config, Optional<VertxBuildItem> vertx, CertificateRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            List<TlsCertificateBuildItem> otherCertificates,
            ShutdownContextBuildItem shutdown) {

        if (vertx.isPresent()) {
            recorder.validateCertificates(config, vertx.get().getVertx(), shutdown);
        }

        for (TlsCertificateBuildItem certificate : otherCertificates) {
            recorder.register(certificate.name, certificate.supplier);
        }

        Supplier<TlsConfigurationRegistry> supplier = recorder.getSupplier();

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(TlsConfigurationRegistry.class)
                .supplier(supplier)
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit();

        syntheticBeans.produce(configurator.done());

        return new TlsRegistryBuildItem(supplier);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = LetsEncryptEnabled.class)
    void createManagementRoutes(BuildProducer<RouteBuildItem> routes,
            LetsEncryptRecorder recorder,
            TlsRegistryBuildItem registryBuildItem) {

        // Check if Vert.x Web is present
        if (!QuarkusClassLoader.isClassPresentAtRuntime("io.vertx.ext.web.Router")) {
            throw new ConfigurationException("Cannot use Let's Encrypt without the quarkus-vertx-http extension");
        }

        recorder.initialize(registryBuildItem.registry());

        // Route to handle the Let's Encrypt challenge - primary HTTP server
        routes.produce(RouteBuildItem.newAbsoluteRoute("/.well-known/acme-challenge/:token")
                .withRequestHandler(recorder.challengeHandler())
                .build());

        // Route to configure the Let's Encrypt challenge - management server
        routes.produce(RouteBuildItem.newManagementRoute("lets-encrypt/challenge")
                .withRequestHandler(recorder.chalengeAdminHandler())
                .withRouteCustomizer(recorder.setupCustomizer())
                .build());

        // Route to refresh the certificates - management server
        routes.produce(RouteBuildItem.newManagementRoute("lets-encrypt/certs")
                .withRequestHandler(recorder.reload())
                .build());
    }

}
