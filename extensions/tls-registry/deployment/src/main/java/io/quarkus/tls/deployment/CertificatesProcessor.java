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
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.runtime.LaunchMode;
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
import io.quarkus.tls.runtime.config.LetsEncryptClientConfig;
import io.quarkus.tls.runtime.config.TlsConfig;
import io.quarkus.tls.runtime.letsencrypt.LetsEncryptCertificateManager;
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
            ServiceRegistrar reg,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            List<TlsCertificateBuildItem> otherCertificates,
            LetsEncryptBuildTimeConfig letsEncryptConfig) {

        Set<String> providerBucketNames = Set.of();
        if (vertx.isPresent()) {
            Set<String> names = getProviderBucketNames(beanDiscovery);
            // the Let's Encrypt client provides the key store of its TLS configuration
            if (letsEncryptConfig.client().enabled()) {
                letsEncryptConfig.client().tlsConfigurationName().ifPresent(names::add);
            }
            providerBucketNames = Set.copyOf(names);
        }
        Set<String> capturedProviderBucketNames = providerBucketNames;
        String letsEncryptTlsConfigurationName = letsEncryptConfig.client().tlsConfigurationName()
                .orElse(TlsConfig.DEFAULT_NAME);

        // capture additional certificate suppliers for the lambda;
        // suppliers must be records or other capturable types
        Map<String, Supplier<TlsConfiguration>> capturedCerts = Map.copyOf(
                otherCertificates.stream().collect(Collectors.toMap(c -> c.name, c -> c.supplier)));

        reg
                .forService(TlsConfigurationRegistry.class)
                .atPhase(Phase.INFRASTRUCTURE)
                .require(TlsConfig.class)
                .request(Vertx.class)
                .request(LetsEncryptCertificateManager.class)
                .onStart((ctx, tlsConfig, vertxOpt, letsEncrypt) -> {
                    CertificateRegistryImpl registry = new CertificateRegistryImpl(tlsConfig,
                            letsEncrypt.isPresent()
                                    ? Map.of(letsEncryptTlsConfigurationName, letsEncrypt.get())
                                    : Map.of());
                    vertxOpt.ifPresent(v -> {
                        registry.validateCertificates(capturedProviderBucketNames, (Vertx) v);
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
                .runtimeValue(reg.serviceAsRuntimeValue(TlsConfigurationRegistry.class))
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .done());

        return new TlsRegistryBuildItem(
                reg.serviceAsRecorderSupplier(TlsConfigurationRegistry.class));
    }

    @BuildStep(onlyIf = LetsEncryptClientEnabled.class)
    void letsEncryptClient(LetsEncryptBuildTimeConfig config, ServiceRegistrar serviceRegistrar,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {
        // the client calls the ACME server over HTTPS
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem("tls-registry-lets-encrypt-client"));

        String tlsConfigurationName = config.client().tlsConfigurationName().orElse(TlsConfig.DEFAULT_NAME);

        // the TLS registry requests the manager, which provides the key store of the TLS configuration
        serviceRegistrar.forService(LetsEncryptCertificateManager.class)
                .atPhase(Phase.INFRASTRUCTURE)
                .require(LetsEncryptClientConfig.class)
                .require(Vertx.class)
                .onStart((ctx, clientConfig, vertx) -> LetsEncryptCertificateManager.create(vertx, clientConfig,
                        tlsConfigurationName, LaunchMode.current()));

        // the renewal checks start once the HTTP server serves the challenges
        serviceRegistrar.forService("io.quarkus.tls.lets-encrypt.client.start")
                .atPhase(Phase.SERVING)
                .require(LetsEncryptCertificateManager.class)
                .require(TlsConfigurationRegistry.class)
                .onStart((ctx, manager, registry) -> {
                    manager.start(registry);
                    ctx.onStop(manager::close);
                });
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = LetsEncryptRoutesEnabled.class)
    void createManagementRoutes(BuildProducer<RouteBuildItem> routes,
            LetsEncryptRecorder recorder,
            TlsRegistryBuildItem registryBuildItem,
            LetsEncryptBuildTimeConfig config) {

        // Check if Vert.x Web is present
        if (!QuarkusClassLoader.isClassPresentAtRuntime("io.vertx.ext.web.Router")) {
            throw new ConfigurationException("Cannot use Let's Encrypt without the quarkus-vertx-http extension");
        }

        recorder.initialize(registryBuildItem.registry());

        // Route to handle the Let's Encrypt challenge - primary HTTP server
        routes.produce(RouteBuildItem.newAbsoluteRoute("/.well-known/acme-challenge/:token")
                .withRequestHandler(recorder.challengeHandler())
                .build());

        // the Quarkus CLI drives the flow through the management routes; the client of the application does not
        if (!config.enabled()) {
            return;
        }

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
