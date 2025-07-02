package io.quarkus.tls.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.deployment.spi.TlsCertificateBuildItem;
import io.quarkus.tls.deployment.spi.TlsRegistryBuildItem;
import io.quarkus.tls.runtime.CertificateRecorder;
import io.quarkus.tls.runtime.KeyStoreProvider;
import io.quarkus.tls.runtime.LetsEncryptRecorder;
import io.quarkus.tls.runtime.TrustStoreProvider;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.quarkus.vertx.http.deployment.spi.RouteBuildItem;
import io.smallrye.common.annotation.Identifier;

public class CertificatesProcessor {

    static final DotName IDENTIFIER_DOT_NAME = DotName.createSimple(Identifier.class);
    static final DotName KEYSTORE_PROVIDER_DOT_NAME = DotName.createSimple(KeyStoreProvider.class);
    static final DotName TRUSTSTORE_PROVIDER_DOT_NAME = DotName.createSimple(TrustStoreProvider.class);

    @BuildStep
    public UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(KeyStoreProvider.class, TrustStoreProvider.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public TlsRegistryBuildItem initializeCertificate(
            Optional<VertxBuildItem> vertx,
            BeanDiscoveryFinishedBuildItem beadDiscovery,
            CertificateRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            List<TlsCertificateBuildItem> otherCertificates,
            ShutdownContextBuildItem shutdown) {

        if (vertx.isPresent()) {
            var providerBucketNames = getProviderBucketNames(beadDiscovery);
            recorder.validateCertificates(providerBucketNames, vertx.get().getVertx(), shutdown);
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

    static Set<String> getProviderBucketNames(BeanDiscoveryFinishedBuildItem beanDiscovery) {
        var bucketNames = new HashSet<String>();
        for (var beanInfo : beanDiscovery.getBeans()) {
            if (beanInfo.hasType(KEYSTORE_PROVIDER_DOT_NAME) || beanInfo.hasType(TRUSTSTORE_PROVIDER_DOT_NAME)) {
                var identifier = beanInfo.getQualifier(IDENTIFIER_DOT_NAME);
                if (identifier.isPresent()) {
                    bucketNames.add(identifier.get().value().asString());
                }
            }
        }
        return bucketNames;
    }
}
