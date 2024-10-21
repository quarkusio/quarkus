package io.quarkus.tekton.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.dekorate.BuildImage;
import io.dekorate.tekton.configurator.ApplyTektonImageBuilderInfoConfigurator;
import io.dekorate.tekton.configurator.UseLocaDockerConfigJsonConfigurator;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.kubernetes.spi.ConfiguratorBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesAdditionalResourceBuildItem;

public class TektonProcessor {

    private static final Logger log = Logger.getLogger(TektonProcessor.class);
    private static final String TEKTON_TASK = "tekton-task";

    @BuildStep
    public void checkVanillaKubernetes(BuildProducer<KubernetesAdditionalResourceBuildItem> additionalResources) {
        additionalResources.produce(new KubernetesAdditionalResourceBuildItem("tekton-task"));
        additionalResources.produce(new KubernetesAdditionalResourceBuildItem("tekton-task-run"));
    }

    @BuildStep(onlyIf = { IsNormal.class, TektonEnabled.class }, onlyIfNot = NativeBuild.class)
    public List<ConfiguratorBuildItem> createJvmConfigurators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget, TektonConfig config) {
        List<ConfiguratorBuildItem> result = new ArrayList<>();
        String task = TektonUtil.monolithTaskName(config);
        result.add(new ConfiguratorBuildItem(new UseLocaDockerConfigJsonConfigurator(config.useLocalDockerConfigJson)));

        Optional<BuildImage> buildImage = TektonUtil.getBuildImage(outputTarget.getOutputDirectory().toFile());
        String builderImage = config.builderImage.orElse(buildImage.map(BuildImage::getImage).orElse(null));
        String builderCommand = config.builderCommand.orElse(buildImage.map(BuildImage::getCommand).orElse(null));
        List<String> builderArguments = config.builderArguments
                .orElse(Arrays.asList(buildImage.map(BuildImage::getArguments).orElse(new String[0])));
        result.add(new ConfiguratorBuildItem(new ApplyTektonImageBuilderInfoConfigurator(builderImage, builderCommand,
                builderArguments.toArray(new String[builderArguments.size()]))));

        return result;
    }

    @BuildStep(onlyIf = { IsNormal.class, TektonEnabled.class, NativeBuild.class })
    public List<ConfiguratorBuildItem> createNativeConfigurators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget, TektonConfig config) {
        List<ConfiguratorBuildItem> result = new ArrayList<>();
        String task = TektonUtil.monolithTaskName(config);
        result.add(new ConfiguratorBuildItem(new UseLocaDockerConfigJsonConfigurator(config.useLocalDockerConfigJson)));

        Optional<BuildImage> buildImage = TektonUtil.getBuildImage(outputTarget.getOutputDirectory().toFile());
        String builderImage = config.builderImage.orElse(buildImage.map(BuildImage::getImage).orElse(null));
        String builderCommand = config.builderCommand.orElse(buildImage.map(BuildImage::getCommand).orElse(null));
        List<String> builderArguments = config.builderArguments
                .orElse(Arrays.asList(buildImage.map(BuildImage::getArguments).orElse(new String[0])));
        result.add(new ConfiguratorBuildItem(new ApplyTektonImageBuilderInfoConfigurator(builderImage, builderCommand,
                builderArguments.toArray(new String[builderArguments.size()]))));

        return result;
    }

    @BuildStep(onlyIf = { IsNormal.class, TektonEnabled.class }, onlyIfNot = NativeBuild.class)
    public List<DecoratorBuildItem> createJvmTaskDecorators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget, TektonConfig config) {
        List<DecoratorBuildItem> result = new ArrayList<>();
        String task = TektonUtil.monolithTaskName(config);
        result.add(new DecoratorBuildItem(TEKTON_TASK,
                new ApplyParamToTaskDecorator(task, "pathToDockerfile", "Path to Dockerfile", config.jvmDockerfile)));
        return result;
    }

    @BuildStep(onlyIf = { IsNormal.class, TektonEnabled.class, NativeBuild.class })
    public List<DecoratorBuildItem> createNativeTaskDecorators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget, TektonConfig config) {
        List<DecoratorBuildItem> result = new ArrayList<>();
        String task = TektonUtil.monolithTaskName(config);
        result.add(new DecoratorBuildItem(TEKTON_TASK,
                new ApplyParamToTaskDecorator(task, "pathToDockerfile", "Path to Dockerfile", config.nativeDockerfile)));
        return result;
    }
}
