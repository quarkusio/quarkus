package io.quarkus.kubernetes.config.deployment;

import java.util.Arrays;
import java.util.Collections;

import org.jboss.logmanager.Level;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceValueBuildItem;
import io.quarkus.kubernetes.client.runtime.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.config.runtime.KubernetesConfigBuildTimeConfig;
import io.quarkus.kubernetes.config.runtime.KubernetesConfigRecorder;
import io.quarkus.kubernetes.config.runtime.KubernetesConfigSourceConfig;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;
import io.quarkus.runtime.TlsConfig;

public class KubernetesConfigProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public RunTimeConfigurationSourceValueBuildItem configure(KubernetesConfigRecorder recorder,
            KubernetesConfigSourceConfig config, KubernetesConfigBuildTimeConfig buildTimeConfig,
            KubernetesClientBuildConfig clientConfig,
            TlsConfig tlsConfig) {
        return new RunTimeConfigurationSourceValueBuildItem(
                recorder.configSources(config, buildTimeConfig, clientConfig, tlsConfig));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void handleAccessToSecrets(KubernetesConfigSourceConfig config,
            KubernetesConfigBuildTimeConfig buildTimeConfig,
            BuildProducer<KubernetesRoleBuildItem> roleProducer,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindingProducer,
            KubernetesConfigRecorder recorder) {
        if (buildTimeConfig.secretsEnabled) {
            roleProducer.produce(new KubernetesRoleBuildItem("view-secrets", Collections.singletonList(
                    new KubernetesRoleBuildItem.PolicyRule(
                            Collections.singletonList(""),
                            Collections.singletonList("secrets"),
                            Arrays.asList("get")))));
            roleBindingProducer.produce(new KubernetesRoleBindingBuildItem("view-secrets", false));
        }

        recorder.warnAboutSecrets(config, buildTimeConfig);
    }

    // done in order to ensure that http logs aren't shown by default which happens because of the interplay between
    // not yet setup logging (as the bootstrap config runs before logging is setup) and the configuration
    // of the okhttp3.logging.HttpLoggingInterceptor by io.fabric8.kubernetes.client.utils.HttpClientUtils
    @BuildStep
    public void produceLoggingCategories(BuildProducer<LogCategoryBuildItem> categories) {
        categories.produce(new LogCategoryBuildItem("okhttp3.OkHttpClient", Level.WARN));
    }
}
