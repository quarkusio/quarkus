package io.quarkus.tls;

import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.tls.runtime.CertificateRecorder;
import io.quarkus.tls.runtime.config.TlsConfig;
import io.quarkus.vertx.deployment.VertxBuildItem;

public class CertificatesProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public TlsRegistryBuildItem initializeCertificate(
            TlsConfig config, VertxBuildItem vertx, CertificateRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            List<TlsCertificateBuildItem> otherCertificates) {
        recorder.validateCertificates(config, vertx.getVertx());
        for (TlsCertificateBuildItem certificate : otherCertificates) {
            recorder.register(certificate.name, certificate.supplier);
        }

        Supplier<TlsConfigurationRegistry> supplier = recorder.getSupplier();

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(TlsConfigurationRegistry.class)
                .supplier(supplier)
                .scope(ApplicationScoped.class)
                .unremovable()
                .setRuntimeInit();

        syntheticBeans.produce(configurator.done());

        return new TlsRegistryBuildItem(supplier);
    }

}
