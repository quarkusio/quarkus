package io.quarkus.tls.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.KeyStoreFactory;
import io.quarkus.tls.KeyStoreProvider;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.TrustStoreFactory;
import io.quarkus.tls.TrustStoreProvider;
import io.quarkus.tls.deployment.spi.TlsCertificateBuildItem;
import io.quarkus.tls.deployment.spi.TlsRegistryBuildItem;
import io.quarkus.tls.runtime.CertificateRegistryImpl;
import io.quarkus.tls.runtime.LetsEncryptRecorder;
import io.quarkus.tls.runtime.config.TlsConfig;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.quarkus.vertx.http.deployment.spi.RouteBuildItem;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;

public class CertificatesProcessor {

    static final DotName IDENTIFIER_DOT_NAME = DotName.createSimple(Identifier.class);
    static final DotName KEYSTORE_PROVIDER_DOT_NAME = DotName.createSimple(KeyStoreProvider.class);
    static final DotName TRUSTSTORE_PROVIDER_DOT_NAME = DotName.createSimple(TrustStoreProvider.class);

    @BuildStep
    public UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(KeyStoreProvider.class, TrustStoreProvider.class,
                KeyStoreFactory.class, TrustStoreFactory.class);
    }

    @BuildStep
    public TlsRegistryBuildItem initializeCertificate(
            Optional<VertxBuildItem> vertx,
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            ActionBuilder action,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            List<TlsCertificateBuildItem> otherCertificates) {

        Set<String> providerBucketNames = vertx.isPresent()
                ? Set.copyOf(getProviderBucketNames(beanDiscovery))
                : Set.of();

        // capture additional certificate suppliers for the lambda;
        // suppliers must be records or other capturable types
        Map<String, Supplier<TlsConfiguration>> capturedCerts = Map.copyOf(
                otherCertificates.stream().collect(Collectors.toMap(c -> c.name, c -> c.supplier)));

        action
                .forService(TlsConfigurationRegistry.class)
                .atPhase(Phase.INFRASTRUCTURE)
                .require(TlsConfig.class)
                .request(Vertx.class)
                .action((ctx, tlsConfig, vertxOpt) -> {
                    CertificateRegistryImpl registry = new CertificateRegistryImpl(tlsConfig);
                    vertxOpt.ifPresent(v -> {
                        registry.validateCertificates(providerBucketNames, (Vertx) v);
                        ctx.onStop(registry::closeReloader);
                    });
                    for (var entry : capturedCerts.entrySet()) {
                        registry.register(entry.getKey(), entry.getValue().get());
                    }
                    return registry;
                });

        // temporary bridge: supplier for unconverted consumers
        syntheticBeans.produce(SyntheticBeanBuildItem
                .configure(TlsConfigurationRegistry.class)
                .runtimeValue(action.serviceAsRuntimeValue(TlsConfigurationRegistry.class))
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .done());

        return new TlsRegistryBuildItem(
                action.serviceAsRecorderSupplier(TlsConfigurationRegistry.class));
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
