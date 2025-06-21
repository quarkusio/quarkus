package io.quarkus.kubernetes.config.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.kubernetes.config.runtime.KubernetesConfigBuildTimeConfig;
import io.quarkus.kubernetes.config.runtime.KubernetesConfigRecorder;
import io.quarkus.kubernetes.config.runtime.KubernetesConfigSourceFactoryBuilder;
import io.quarkus.kubernetes.config.runtime.SecretsRoleConfig;
import io.quarkus.kubernetes.spi.KubernetesClusterRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesServiceAccountBuildItem;
import io.quarkus.kubernetes.spi.PolicyRule;

public class KubernetesConfigProcessor {

    private static final String ANY_TARGET = null;
    private static final List<PolicyRule> POLICY_RULE_FOR_ROLE = List.of(new PolicyRule(
            List.of(""),
            List.of("secrets"),
            List.of("get")));

    @BuildStep
    void configFactory(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(KubernetesConfigSourceFactoryBuilder.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void handleAccessToSecrets(
            KubernetesConfigBuildTimeConfig buildTimeConfig,
            BuildProducer<KubernetesRoleBuildItem> roleProducer,
            BuildProducer<KubernetesClusterRoleBuildItem> clusterRoleProducer,
            BuildProducer<KubernetesServiceAccountBuildItem> serviceAccountProducer,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindingProducer,
            KubernetesConfigRecorder recorder) {
        if (buildTimeConfig.secretsEnabled()) {
            SecretsRoleConfig roleConfig = buildTimeConfig.secretsRoleConfig();
            String roleName = roleConfig.name();
            if (roleConfig.generate()) {
                if (roleConfig.clusterWide()) {
                    clusterRoleProducer.produce(new KubernetesClusterRoleBuildItem(roleName,
                            POLICY_RULE_FOR_ROLE,
                            ANY_TARGET));
                } else {
                    roleProducer.produce(new KubernetesRoleBuildItem(roleName,
                            roleConfig.namespace().orElse(null),
                            POLICY_RULE_FOR_ROLE,
                            ANY_TARGET));
                }
            }

            serviceAccountProducer.produce(new KubernetesServiceAccountBuildItem(true));
            roleBindingProducer.produce(new KubernetesRoleBindingBuildItem(roleName, roleConfig.clusterWide()));
        }

        recorder.warnAboutSecrets();
    }
}
