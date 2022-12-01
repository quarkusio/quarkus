package io.quarkus.kubernetes.deployment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
import io.quarkus.kubernetes.spi.KubernetesInitContainerBuildItem;
import io.quarkus.kubernetes.spi.KubernetesJobBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;

public class InitTaskProcessor {

    @BuildStep
    public void process(ContainerImageInfoBuildItem image, List<InitTaskBuildItem> initTasks,
            BuildProducer<KubernetesJobBuildItem> jobs,
            BuildProducer<KubernetesInitContainerBuildItem> initContainers,
            BuildProducer<KubernetesEnvBuildItem> env,
            BuildProducer<KubernetesRoleBuildItem> roles,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindings) {
        initTasks.forEach(task -> {
            initContainers.produce(KubernetesInitContainerBuildItem.create("groundnuty/k8s-wait-for:1.3")
                    .withArguments(Arrays.asList("job", task.getName())));

            jobs.produce(KubernetesJobBuildItem.create(image.getImage())
                    .withName(task.getName())
                    .withEnvVars(task.getTaskEnvVars())
                    .withCommand(task.getCommand())
                    .withArguments(task.getArguments())
                    .withSharedEnvironment(task.isSharedEnvironment())
                    .withSharedFilesystem(task.isSharedFilesystem()));

            task.getAppEnvVars().forEach((k, v) -> {
                env.produce(KubernetesEnvBuildItem.createSimpleVar(k, v, null, false));
            });

            roles.produce(new KubernetesRoleBuildItem("view-jobs", Collections.singletonList(
                    new KubernetesRoleBuildItem.PolicyRule(
                            Collections.singletonList("batch"),
                            Collections.singletonList("jobs"),
                            List.of("get")))));
            roleBindings.produce(new KubernetesRoleBindingBuildItem("view-jobs", false));

        });
    }

    // # The init containers
    // initContainers:
    // - name: "{{ .Chart.Name }}-init"
    //   image: "groundnuty/k8s-wait-for:1.3"
    //   imagePullPolicy: {{ .Values.image.pullPolicy }}
    //   args:
    //   - "job"
    //   - "{{ .Release.Name }}-test-app-cli-{{ .Release.Revision}}"
}
