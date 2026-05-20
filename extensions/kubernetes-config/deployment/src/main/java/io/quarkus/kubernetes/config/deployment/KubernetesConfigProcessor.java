package io.quarkus.kubernetes.config.deployment;

import java.util.Collections;
import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
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
import io.quarkus.kubernetes.spi.RoleRef;
import io.quarkus.kubernetes.spi.Subject;

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
            ApplicationInfoBuildItem applicationInfo,
            BuildProducer<KubernetesRoleBuildItem> roleProducer,
            BuildProducer<KubernetesClusterRoleBuildItem> clusterRoleProducer,
            BuildProducer<KubernetesServiceAccountBuildItem> serviceAccountProducer,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindingProducer,
            KubernetesConfigRecorder recorder) {
        if (buildTimeConfig.secretsEnabled()) {
            SecretsRoleConfig roleConfig = buildTimeConfig.secretsRoleConfig();

            String roleName = roleConfig.generateName()
                    ? applicationInfo.getName() + "-" + roleConfig.name()
                    : roleConfig.name();

            List<PolicyRule> policyRules = roleConfig.secretNames()
                    .filter(names -> !names.isEmpty())
                    .map(names -> List.of(new PolicyRule(
                            List.of(""),
                            null,
                            names,
                            List.of("secrets"),
                            List.of("get"))))
                    .orElse(POLICY_RULE_FOR_ROLE);

            if (roleConfig.generate()) {
                if (roleConfig.clusterWide()) {
                    clusterRoleProducer.produce(new KubernetesClusterRoleBuildItem(roleName,
                            policyRules,
                            ANY_TARGET));
                } else {
                    roleProducer.produce(new KubernetesRoleBuildItem(roleName,
                            roleConfig.namespace().orElse(null),
                            policyRules,
                            ANY_TARGET));
                }
            }

            serviceAccountProducer.produce(new KubernetesServiceAccountBuildItem(true));
            String bindingName = roleConfig.generateName() ? roleName : null;
            roleBindingProducer.produce(new KubernetesRoleBindingBuildItem(
                    bindingName, null, ANY_TARGET, Collections.emptyMap(),
                    new RoleRef(roleName, roleConfig.clusterWide()),
                    new Subject("", "ServiceAccount", null, null)));
        }

        recorder.warnAboutSecrets();
    }
}
