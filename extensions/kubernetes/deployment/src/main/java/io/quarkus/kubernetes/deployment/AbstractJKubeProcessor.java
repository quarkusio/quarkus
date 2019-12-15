package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.FakeMavenMojoEnvironment.PLEXUS_CONTAINER;
import static io.quarkus.kubernetes.deployment.FakeMavenMojoEnvironment.fakeMavenBuildContext;

import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.StreamSupport;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.jkube.kit.build.maven.MavenBuildContext;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.RegistryService;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.build.service.docker.ServiceHubFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpecFactory;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.eclipse.jkube.kit.config.service.BuildService;
import org.eclipse.jkube.kit.config.service.JkubeServiceHub;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

class AbstractJKubeProcessor {

    static final String TARGET_DIR = "target";

    AbstractJKubeProcessor() {
    }

    private static BuildService.BuildServiceConfig initBuildServiceConfig(Path projectDirectory) {
        final MavenBuildContext fakeDockerMojoParameters = fakeMavenBuildContext(projectDirectory);
        return new BuildService.BuildServiceConfig.Builder()
                .dockerBuildContext(new org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext.Builder()
                        .mojoParameters(fakeDockerMojoParameters)
                        .registryConfig(new RegistryService.RegistryConfig.Builder().build())
                        .build())
                .dockerMavenBuildContext(fakeDockerMojoParameters)
                .openshiftBuildStrategy(OpenShiftBuildStrategy.docker)
                .s2iBuildNameSuffix("")
                .buildRecreateMode(BuildRecreateMode.all)
                .buildDirectory(projectDirectory.resolve("target").toString())
                .build();
    }

    private static DockerAccess initDockerAccess(JKubeLogger jKubeLogger)
            throws MojoExecutionException, MojoFailureException, ComponentLookupException {

        return PLEXUS_CONTAINER.lookup(DockerAccessFactory.class).createDockerAccess(
                new DockerAccessFactory.DockerAccessContext.Builder()
                        .log(jKubeLogger)
                        .skipMachine(false)
                        .maxConnections(100)
                        .projectProperties(new Properties())
                        .build());
    }

    private static ServiceHub initServiceHub(JKubeLogger jKubeLogger)
            throws MojoExecutionException, MojoFailureException, ComponentLookupException {

        return PLEXUS_CONTAINER.lookup(ServiceHubFactory.class).createServiceHub(null, null,
                initDockerAccess(jKubeLogger), jKubeLogger, new LogOutputSpecFactory(true, true, ""));
    }

    private static ClusterConfiguration initClusterConfiguration() {
        final Config config = ConfigProvider.getConfig();
        final Properties props = new Properties();
        StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                .forEach(key -> props.put(key, config.getValue(key, String.class)));
        return new ClusterConfiguration.Builder().from(System.getProperties()).from(props).build();
    }

    static ClusterAccess initClusterAccess() {
        return new ClusterAccess(initClusterConfiguration());
    }

    static JkubeServiceHub initJKubeServiceHub(JKubeLogger jKubeLogger, Path projectDirectory)
            throws MojoExecutionException, MojoFailureException, ComponentLookupException {

        return new JkubeServiceHub.Builder()
                .log(jKubeLogger)
                .clusterAccess(initClusterAccess())
                .buildServiceConfig(initBuildServiceConfig(projectDirectory))
                .dockerServiceHub(initServiceHub(jKubeLogger))
                .build();
    }
}
