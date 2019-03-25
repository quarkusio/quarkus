package io.quarkus.kubernetes.deployment;

import static io.quarkus.deployment.builditem.ApplicationInfoBuildItem.UNSET_VALUE;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import io.ap4k.Session;
import io.ap4k.SessionWriter;
import io.ap4k.docker.annotation.EnableDockerBuild;
import io.ap4k.docker.generator.DefaultEnableDockerBuildGenerator;
import io.ap4k.kubernetes.annotation.KubernetesApplication;
import io.ap4k.kubernetes.generator.DefaultKubernetesApplicationGenerator;
import io.ap4k.processor.SimpleFileWriter;
import io.ap4k.project.Project;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;

class KubernetesProcessor {

    @Inject
    BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer;

    @Inject
    BuildProducer<FeatureBuildItem> featureProducer;

    @BuildStep
    public void build(ApplicationInfoBuildItem applicationInfo,
            KubernetesConfig kubernetesConfig,
            List<KubernetesPortBuildItem> kubernetesPortBuildItems) {

        if (kubernetesPortBuildItems.isEmpty()) {
            return;
        }

        //this is done to prevent ap4k from failing when the necessary input has not been provided (during tests or devmode for example)
        if (UNSET_VALUE.equals(applicationInfo.getWiringClassesDir()) || UNSET_VALUE.equals(applicationInfo.getFinalName())) {
            return;
        }

        // The resources that ap4k's execution will result in, will later-on be written
        // by quarkus in the 'wiring-classes' directory
        // The location is needed in order to properly support s2i build triggering
        final Path rootPath = Paths.get(applicationInfo.getWiringClassesDir());
        if (rootPath.toString().isEmpty()) {
            return;
        }
        final Path ap4kRoot = rootPath.resolve("META-INF/ap4k");

        // by passing false to SimpleFileWriter, we ensure that no files are actually written during this phase
        final SessionWriter sessionWriter = new SimpleFileWriter(ap4kRoot, false);
        final String packaging = "jar";
        sessionWriter.setProject(new Project(Paths.get(applicationInfo.getBaseDir()),
                new io.ap4k.project.BuildInfo(
                        applicationInfo.getName(), applicationInfo.getVersion(), packaging,
                        rootPath.getParent().resolve(applicationInfo.getFinalName() + "-runner." + packaging), rootPath)));
        final Session session = Session.getSession();
        session.setWriter(sessionWriter);

        enableDocker(kubernetesConfig);
        final Map<String, Integer> ports = verifyPorts(kubernetesPortBuildItems);
        enableKubernetes(applicationInfo, kubernetesConfig, ports);

        // write the generated resources to the filesystem
        final Map<String, String> generatedResourcesMap = session.close();
        for (String generatedResourceFullPath : generatedResourcesMap.keySet()) {
            generatedResourceProducer.produce(
                    new GeneratedResourceBuildItem(
                            // we need to make sure we are only passing the relative path to the build item
                            generatedResourceFullPath.replace(rootPath.toString() + "/", ""),
                            generatedResourcesMap.get(generatedResourceFullPath).getBytes()));
        }

        featureProducer.produce(new FeatureBuildItem(FeatureBuildItem.KUBERNETES));
    }

    private Map<String, Integer> verifyPorts(List<KubernetesPortBuildItem> kubernetesPortBuildItems) {
        final Map<String, Integer> result = new HashMap<>();
        final Set<Integer> usedPorts = new HashSet<>();
        for (KubernetesPortBuildItem entry : kubernetesPortBuildItems) {
            final String name = entry.getName();
            if (result.containsKey(name)) {
                throw new IllegalArgumentException(
                        "All Kubernetes ports must have unique names - " + name + "has been used multiple times");
            }
            final Integer port = entry.getPort();
            if (usedPorts.contains(port)) {
                throw new IllegalArgumentException(
                        "All Kubernetes ports must be unique - " + port + "has been used multiple times");
            }
            result.put(name, port);
            usedPorts.add(port);
        }
        return result;
    }

    private void enableDocker(KubernetesConfig kubernetesConfig) {
        final Map<String, Object> dockerProperties = new HashMap<>();
        dockerProperties.put("dockerFile", "src/main/docker/Dockerfile.jvm");
        dockerProperties.put("registry", kubernetesConfig.docker.registry);
        dockerProperties.put("autoBuildEnabled", kubernetesConfig.docker.build);
        dockerProperties.put("autoPushEnabled", kubernetesConfig.docker.push);

        final DefaultEnableDockerBuildGenerator generator = new DefaultEnableDockerBuildGenerator();
        final Map<String, Object> generatorInput = new HashMap<>();
        generatorInput.put(EnableDockerBuild.class.getName(), dockerProperties);
        generator.add(generatorInput);
    }

    private void enableKubernetes(ApplicationInfoBuildItem applicationInfo, KubernetesConfig kubernetesConfig,
            Map<String, Integer> portsMap) {
        final Map<String, Object> kubernetesProperties = new HashMap<>();
        kubernetesProperties.put("group", applicationInfo.getGroup());
        kubernetesProperties.put("name", applicationInfo.getName());
        kubernetesProperties.put("version", applicationInfo.getVersion());
        kubernetesProperties.put("autoDeployEnabled", kubernetesConfig.deploy);

        final List<Map<String, Object>> ports = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : portsMap.entrySet()) {
            final Map<String, Object> portProperties = new HashMap<>();
            portProperties.put("name", entry.getKey());
            portProperties.put("containerPort", entry.getValue());
            ports.add(portProperties);
        }

        kubernetesProperties.put("ports", toArray(ports));

        final DefaultKubernetesApplicationGenerator generator = new DefaultKubernetesApplicationGenerator();
        final Map<String, Object> generatorInput = new HashMap<>();
        generatorInput.put(KubernetesApplication.class.getName(), kubernetesProperties);
        generator.add(generatorInput);
    }

    private <T> T[] toArray(List<T> list) {
        Class clazz = list.get(0).getClass();
        T[] array = (T[]) java.lang.reflect.Array.newInstance(clazz, list.size());
        return list.toArray(array);
    }
}
