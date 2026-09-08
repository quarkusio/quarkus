package io.quarkus.kubernetes.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import io.quarkus.kubernetes.spi.KubernetesServiceAccountBuildItem;
import io.quarkus.kubernetes.spi.PolicyRule;
import io.quarkus.kubernetes.spi.RoleRef;
import io.quarkus.kubernetes.spi.Subject;

public class InitTaskProcessor {

    private static final String INIT_CONTAINER_WAITER_NAME = "wait-for-";

    public static void process(
            String target, // kubernetes, openshift, etc.
            String name,
            ContainerImageInfoBuildItem image,
            List<InitTaskBuildItem> initTasks,
            InitTaskConfig initTaskDefaults,
            Map<String, InitTaskConfig> initTasksConfig,
            BuildProducer<KubernetesJobBuildItem> jobs,
            BuildProducer<KubernetesInitContainerBuildItem> initContainers,
            BuildProducer<KubernetesEnvBuildItem> env,
            BuildProducer<KubernetesRoleBuildItem> roles,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindings,
            BuildProducer<KubernetesServiceAccountBuildItem> serviceAccount,
            BuildProducer<DecoratorBuildItem> decorators) {

        boolean generateRoleForJobs = false;
        for (InitTaskBuildItem task : initTasks) {
            String taskName = task.getName()
                    //Strip appplication.name prefix and init suffix (for compatibility with previous versions)
                    .replaceAll("^" + Pattern.quote(name + "-"), "")
                    .replaceAll(Pattern.quote("-init") + "$", "");
            String jobName = name + "-" + taskName + "-init";

            InitTaskConfig config = initTasksConfig.getOrDefault(taskName, initTaskDefaults);
            if (config == null || config.enabled()) {
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

                String waitForImage = config.waitForContainer().image();
                initContainers
                        .produce(KubernetesInitContainerBuildItem.create(INIT_CONTAINER_WAITER_NAME + taskName, waitForImage)
                                .withImagePullPolicy(config.waitForContainer().imagePullPolicy().name())
                                .withTarget(target)
                                .withArguments(List.of("job", jobName)));
            }
        }

        if (generateRoleForJobs) {
            InitTaskConfig.InitTaskRbacConfig rbac = initTaskDefaults.rbac();
            // Dynamic default: {application-name}-view-jobs. When set, use the exact name.
            String roleName = rbac.name().orElse(name + "-view-jobs");

            if (rbac.generate()) {
                roles.produce(new KubernetesRoleBuildItem(roleName, Collections.singletonList(
                        new PolicyRule(
                                Collections.singletonList("batch"),
                                Collections.singletonList("jobs"),
                                List.of("get"))),
                        target));
            }

            // Set the binding name explicitly so it stays in sync with the Role name (and so the
            // default {app}-view-jobs Role is not double-prefixed as {app}-{app}-view-jobs).
            roleBindings.produce(new KubernetesRoleBindingBuildItem(
                    roleName, null, target, Collections.emptyMap(),
                    new RoleRef(roleName, false),
                    new Subject("", "ServiceAccount", null, null)));
            serviceAccount.produce(new KubernetesServiceAccountBuildItem(true));
        }
    }
}
