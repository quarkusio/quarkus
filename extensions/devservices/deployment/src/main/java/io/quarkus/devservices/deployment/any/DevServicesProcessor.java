package io.quarkus.devservices.deployment.any;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
public class DevServicesProcessor {

    private static final Logger log = Logger.getLogger(DevServicesProcessor.class);

    @BuildStep
    public void createContainers(BuildProducer<DevServicesResultBuildItem> devServicesResultProducer,
            DevServicesConfig devServicesConfig) {

        Map<String, DevServiceConfig> devservices = devServicesConfig.devservice();
        if (devservices != null && !devservices.isEmpty()) {
            Set<Map.Entry<String, DevServiceConfig>> devservicesSet = devservices.entrySet();
            for (Map.Entry<String, DevServiceConfig> devservice : devservicesSet) {
                String name = devservice.getKey();
                DevServiceConfig devServiceConfig = devservice.getValue();
                createContainer(devServicesResultProducer, name, devServiceConfig);
            }
        }
    }

    private void createContainer(BuildProducer<DevServicesResultBuildItem> devServicesResultProducer, String name,
            DevServiceConfig config) {
        if (config.enabled() && config.imageName().isPresent()) {
            DockerImageName dockerImageName = DockerImageName
                    .parse(config.imageName().get());

            AnyContainer container = new AnyContainer(dockerImageName, config.containerPort(), config.mappedPort(),
                    config.sharedNetwork());
            Map<String, String> props = new HashMap<>();

            // Reuse
            // If not provided, and the mapped port is configured, we want reuse true as a default, else false
            if (config.reuse().isPresent()) {
                container = container.withReuse(config.reuse().get());
            } else if (config.mappedPort().isPresent()) {
                container = container.withReuse(true);
            } else {
                container = container.withReuse(false);
            }

            // Access to host
            container = container.withAccessToHost(config.accessToHost());

            // Labels
            container = container.withLabel("quarkus-dev-services", name);
            Map<String, LabelConfig> labels = config.label().labels();
            if (labels != null && !labels.isEmpty()) {
                Set<Map.Entry<String, LabelConfig>> labelsSet = labels.entrySet();
                for (Map.Entry<String, LabelConfig> label : labelsSet) {
                    String labelName = label.getKey();
                    LabelConfig labelConfig = label.getValue();
                    if (labelConfig.value().isPresent()) {
                        String labelValue = labelConfig.value().get();
                        container = container.withLabel(labelName, labelValue);
                    }
                }
            }

            // Env vars
            Map<String, EnvValueConfig> envvars = config.env().vars();
            if (envvars != null && !envvars.isEmpty()) {
                Set<Map.Entry<String, EnvValueConfig>> envvarsSet = envvars.entrySet();
                for (Map.Entry<String, EnvValueConfig> envvar : envvarsSet) {
                    String envName = envvar.getKey();
                    EnvValueConfig envValueConfig = envvar.getValue();
                    if (envValueConfig.value().isPresent()) {
                        String envVal = envValueConfig.value().get();
                        container = container.withEnv(envName, envVal);
                    }
                }
            }

            // File System bindings
            Map<String, FileSystemBindConfig> fileSystemBindings = config.fileSystemBindings().fileSystemBind();
            if (fileSystemBindings != null && !fileSystemBindings.isEmpty()) {
                Set<Map.Entry<String, FileSystemBindConfig>> fileSystemBindingsSet = fileSystemBindings.entrySet();
                for (Map.Entry<String, FileSystemBindConfig> fileSystemBinding : fileSystemBindingsSet) {
                    String hostPath = getAbsoluteHostPath(fileSystemBinding.getKey());
                    FileSystemBindConfig fileSystemBindConfig = fileSystemBinding.getValue();
                    if (fileSystemBindConfig.containerPath().isPresent()) {
                        String containerPath = fileSystemBindConfig.containerPath().get();
                        container = container.withFileSystemBind(hostPath, containerPath, fileSystemBindConfig.bindMode());
                    }
                }
            }

            // Classpath Resource Mapping
            Map<String, ClasspathResourceMappingConfig> classpathResourceMappings = config.classpathResourceMappings()
                    .classpathResourceMapping();
            if (classpathResourceMappings != null && !classpathResourceMappings.isEmpty()) {
                Set<Map.Entry<String, ClasspathResourceMappingConfig>> classpathResourceMappingsSet = classpathResourceMappings
                        .entrySet();
                for (Map.Entry<String, ClasspathResourceMappingConfig> classpathResourceMapping : classpathResourceMappingsSet) {
                    String resourcePath = classpathResourceMapping.getKey();
                    ClasspathResourceMappingConfig classpathResourceMappingConfig = classpathResourceMapping.getValue();
                    if (classpathResourceMappingConfig.containerPath().isPresent()) {
                        String containerPath = classpathResourceMappingConfig.containerPath().get();
                        container = container.withClasspathResourceMapping(resourcePath, containerPath,
                                classpathResourceMappingConfig.bindMode());
                    }
                }
            }

            // Wait for (TODO: Expand http options ?)
            WaitForConfig waitFor = config.waitFor();
            if (waitFor.defaultWaitStrategy().isPresent() && waitFor.defaultWaitStrategy().get()) {
                container = container.waitingFor(Wait.defaultWaitStrategy());
            }
            if (waitFor.healthcheck().isPresent() && waitFor.healthcheck().get()) {
                container = container.waitingFor(Wait.forHealthcheck());
            }
            if (waitFor.http().isPresent()) {
                container = container.waitingFor(Wait.forHttp(waitFor.http().get()).forStatusCode(200));
            }
            if (waitFor.https().isPresent()) {
                container = container.waitingFor(Wait.forHttps(waitFor.https().get()).forStatusCode(200));
            }
            if (waitFor.listeningPort().isPresent() && waitFor.listeningPort().get()) {
                container = container.waitingFor(Wait.forListeningPort());
            }
            if (waitFor.listeningPorts().isPresent()) {
                int[] a = Arrays.stream(waitFor.listeningPorts().get()).mapToInt(Integer::intValue).toArray();
                container = container.waitingFor(Wait.forListeningPorts(a));
            }
            if (waitFor.logMessage().isPresent()) {
                container = container.waitingFor(
                        Wait.forLogMessage(waitFor.logMessage().get(), waitFor.logMessageTimes()));
            }
            if (waitFor.successfulCommand().isPresent()) {
                container = container.waitingFor(Wait.forSuccessfulCommand(waitFor.successfulCommand().get()));
            }

            // Log
            if (config.captureLog()) {
                container = container.withLogConsumer(new JbossContainerLogConsumer(log).withPrefix(name));
            }

            container.start();

            if (container.getPort() != null) {
                String baseUrl = "http://" + container.getHost() + ":" + container.getPort();
                props.put("quarkus.devservices." + name + ".url", baseUrl);
            }
            devServicesResultProducer
                    .produce(new DevServicesResultBuildItem.RunningDevService(name, container.getContainerId(),
                            container::close, props)
                            .toBuildItem());
        }
    }

    private String getAbsoluteHostPath(String hostPath) {
        if (hostPath.startsWith("/")) {
            return hostPath;
        } else if (hostPath.startsWith("./")) {
            hostPath = hostPath.substring(2);
        }
        String userDir = System.getProperty("user.dir");
        return userDir + "/" + hostPath;

    }

    private static class AnyContainer extends GenericContainer<AnyContainer> {
        final OptionalInt containerPort;
        final boolean sharedNetwork;

        public AnyContainer(DockerImageName image, OptionalInt containerPort, OptionalInt mappedPort,
                boolean sharedNetwork) {
            super(image);
            this.containerPort = containerPort;
            this.sharedNetwork = sharedNetwork;
            if (this.containerPort.isPresent() && mappedPort.isPresent()) {
                super.addFixedExposedPort(mappedPort.getAsInt(), this.containerPort.getAsInt());
            }
        }

        @Override
        protected void configure() {
            if (this.sharedNetwork) {
                withNetwork(Network.SHARED);
            }
            if (this.containerPort.isPresent()) {
                addExposedPorts(containerPort.getAsInt());
            }
        }

        public Integer getPort() {
            if (this.containerPort.isPresent()) {
                return this.getMappedPort(containerPort.getAsInt());
            }
            return null;
        }
    }

}
