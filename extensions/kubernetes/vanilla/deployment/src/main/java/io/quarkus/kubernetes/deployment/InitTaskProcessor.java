package io.quarkus.kubernetes.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import io.quarkus.kubernetes.spi.PolicyRule;

public class InitTaskProcessor {

    private static final String INIT_CONTAINER_WAITER_NAME = "init";
    private static final String INIT_CONTAINER_WAITER_DEFAULT_IMAGE = "groundnuty/k8s-wait-for:no-root-v1.7";

    static void process(
            String target, // kubernetes, openshift, etc.
            String name,
            ContainerImageInfoBuildItem image,
            List<InitTaskBuildItem> initTasks,
            Map<String, InitTaskConfig> initTasksConfig,
            BuildProducer<KubernetesJobBuildItem> jobs,
            BuildProducer<KubernetesInitContainerBuildItem> initContainers,
            BuildProducer<KubernetesEnvBuildItem> env,
            BuildProducer<KubernetesRoleBuildItem> roles,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindings,
            BuildProducer<DecoratorBuildItem> decorators) {

        boolean generateRoleForJobs = false;
        for (InitTaskBuildItem task : initTasks) {
            InitTaskConfig config = initTasksConfig.get(task.getName());
            if (config == null || config.enabled) {
                generateRoleForJobs = true;
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

                initContainers.produce(KubernetesInitContainerBuildItem.create(INIT_CONTAINER_WAITER_NAME,
                        config == null ? INIT_CONTAINER_WAITER_DEFAULT_IMAGE : config.image)
                        .withTarget(target)
                        .withArguments(List.of("job", task.getName())));
            }
        }

        if (generateRoleForJobs) {
            roles.produce(new KubernetesRoleBuildItem("view-jobs", Collections.singletonList(
                    new PolicyRule(
                            Collections.singletonList("batch"),
                            Collections.singletonList("jobs"),
                            List.of("get"))),
                    target));
            roleBindings.produce(new KubernetesRoleBindingBuildItem(null, "view-jobs", false, target));
        }
    }
}
