package io.quarkus.kubernetes.deployment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
import io.quarkus.kubernetes.spi.KubernetesInitContainerBuildItem;
import io.quarkus.kubernetes.spi.KubernetesJobBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;

public class InitTaskProcessor {

    static void process(
            String target, // kubernetes, openshift, etc.
            String name,
            ContainerImageInfoBuildItem image, List<InitTaskBuildItem> initTasks,
            BuildProducer<KubernetesJobBuildItem> jobs,
            BuildProducer<KubernetesInitContainerBuildItem> initContainers,
            BuildProducer<KubernetesEnvBuildItem> env,
            BuildProducer<KubernetesRoleBuildItem> roles,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindings,
            BuildProducer<DecoratorBuildItem> decorators) {

        initTasks.forEach(task -> {
            initContainers.produce(KubernetesInitContainerBuildItem.create("groundnuty/k8s-wait-for:1.3")
                    .withTarget(target)
                    .withArguments(Arrays.asList("job", task.getName())));

            jobs.produce(KubernetesJobBuildItem.create(image.getImage())
                    .withName(task.getName())
                    .withTarget(target)
                    .withEnvVars(task.getTaskEnvVars())
                    .withCommand(task.getCommand())
                    .withArguments(task.getArguments())
                    .withSharedEnvironment(task.isSharedEnvironment())
                    .withSharedFilesystem(task.isSharedFilesystem()));

            task.getAppEnvVars().forEach((k, v) -> {
                decorators.produce(new DecoratorBuildItem(target,
                        new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name, new EnvBuilder()
                                .withName(k)
                                .withValue(v)
                                .build())));

            });

            roles.produce(new KubernetesRoleBuildItem("view-jobs", Collections.singletonList(
                    new KubernetesRoleBuildItem.PolicyRule(
                            Collections.singletonList("batch"),
                            Collections.singletonList("jobs"),
                            List.of("get"))),
                    target));
            roleBindings.produce(new KubernetesRoleBindingBuildItem(null, "view-jobs", false, target));

        });
    }
}
