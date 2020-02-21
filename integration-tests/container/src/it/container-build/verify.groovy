
import io.quarkus.container.deployment.DockerBuild;
import io.quarkus.deployment.pkg.ContainerConfig;
import io.quarkus.deployment.util.ExecUtil;

ContainerConfig config = new ContainerConfig()
config.build = true
DockerBuild dockerBuild = new DockerBuild(config)

if (dockerBuild.getAsBoolean()) {
    assert ExecUtil.exec("docker", "images", "container-build")
    assert ExecUtil.exec("docker", "rmi", "container-build:0.1-SNAPSHOT")
} else assert true
