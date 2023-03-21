package io.quarkus.kubernetes.config.deployment;

import java.util.List;

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
import io.quarkus.kubernetes.config.runtime.SecretsRoleConfig;
import io.quarkus.kubernetes.spi.KubernetesClusterRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesServiceAccountBuildItem;
import io.quarkus.kubernetes.spi.PolicyRule;
import io.quarkus.runtime.TlsConfig;

public class KubernetesConfigProcessor {

    private static final String ANY_TARGET = null;
    private static final List<PolicyRule> POLICY_RULE_FOR_ROLE = List.of(new PolicyRule(
            List.of(""),
            List.of("secrets"),
            List.of("get")));

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
            BuildProducer<KubernetesClusterRoleBuildItem> clusterRoleProducer,
            BuildProducer<KubernetesServiceAccountBuildItem> serviceAccountProducer,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindingProducer,
            KubernetesConfigRecorder recorder) {
        if (buildTimeConfig.secretsEnabled) {
            SecretsRoleConfig roleConfig = buildTimeConfig.secretsRoleConfig;
            String roleName = roleConfig.name;
            if (roleConfig.generate) {
                if (roleConfig.clusterWide) {
                    clusterRoleProducer.produce(new KubernetesClusterRoleBuildItem(roleName,
                            POLICY_RULE_FOR_ROLE,
                            ANY_TARGET));
                } else {
                    roleProducer.produce(new KubernetesRoleBuildItem(roleName,
                            roleConfig.namespace.orElse(null),
                            POLICY_RULE_FOR_ROLE,
                            ANY_TARGET));
                }
            }

            serviceAccountProducer.produce(new KubernetesServiceAccountBuildItem(true));
            roleBindingProducer.produce(new KubernetesRoleBindingBuildItem(roleName, roleConfig.clusterWide));
        }

        recorder.warnAboutSecrets(config, buildTimeConfig);
    }

    // done in order to ensure that http logs aren't shown by default which happens because of the interplay between
    // not yet setup logging (as the bootstrap config runs before logging is set up) and the configuration
    // of the okhttp3.logging.HttpLoggingInterceptor by io.fabric8.kubernetes.client.utils.HttpClientUtils
    @BuildStep
    public void produceLoggingCategories(BuildProducer<LogCategoryBuildItem> categories) {
        categories.produce(new LogCategoryBuildItem("okhttp3.OkHttpClient", Level.WARN));
    }
}
