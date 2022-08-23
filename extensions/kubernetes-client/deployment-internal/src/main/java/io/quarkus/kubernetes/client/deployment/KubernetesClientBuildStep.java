package io.quarkus.kubernetes.client.deployment;

import static io.quarkus.kubernetes.client.runtime.KubernetesClientUtils.*;

import java.util.function.BooleanSupplier;

import org.jboss.logmanager.Level;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.kubernetes.client.runtime.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.runtime.TlsConfig;

public class KubernetesClientBuildStep {

    private KubernetesClientBuildConfig buildConfig;

    @BuildStep
    public KubernetesClientBuildItem process(TlsConfig tlsConfig) {
        return new KubernetesClientBuildItem(createClient(buildConfig, tlsConfig));
    }

    @BuildStep(onlyIfNot = IsStaticLoggerEnabled.class)
    public void produceLoggingCategories(BuildProducer<LogCategoryBuildItem> categories) {
        categories.produce(new LogCategoryBuildItem("okhttp3.OkHttpClient", Level.WARN));
    }

    public static class IsStaticLoggerEnabled implements BooleanSupplier {

        final KubernetesClientBuildConfig config;

        public IsStaticLoggerEnabled(KubernetesClientBuildConfig config) {
            this.config = config;
        }

        @Override
        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
