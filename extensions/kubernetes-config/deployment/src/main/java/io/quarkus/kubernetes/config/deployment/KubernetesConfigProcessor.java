package io.quarkus.kubernetes.config.deployment;

import org.jboss.logmanager.Level;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceValueBuildItem;
import io.quarkus.kubernetes.client.runtime.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.runtime.KubernetesConfigRecorder;
import io.quarkus.kubernetes.client.runtime.KubernetesConfigSourceConfig;

public class KubernetesConfigProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public RunTimeConfigurationSourceValueBuildItem configure(KubernetesConfigRecorder recorder,
            KubernetesConfigSourceConfig config, KubernetesClientBuildConfig clientConfig) {
        return new RunTimeConfigurationSourceValueBuildItem(
                recorder.configSources(config, clientConfig));
    }

    // done in order to ensure that http logs aren't shown by default which happens because of the interplay between
    // not yet setup logging (as the bootstrap config runs before logging is setup) and the configuration
    // of the okhttp3.logging.HttpLoggingInterceptor by io.fabric8.kubernetes.client.utils.HttpClientUtils
    @BuildStep
    public void produceLoggingCategories(BuildProducer<LogCategoryBuildItem> categories) {
        categories.produce(new LogCategoryBuildItem("okhttp3.OkHttpClient", Level.WARN));
    }
}
