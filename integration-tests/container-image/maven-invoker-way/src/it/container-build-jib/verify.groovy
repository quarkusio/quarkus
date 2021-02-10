import io.quarkus.deployment.util.ExecUtil

import java.nio.file.Paths

try {
    ExecUtil.exec("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

String group = System.getProperty("user.name")
assert ExecUtil.exec("docker", "images", group + "/container-build-jib")
assert ExecUtil.exec("docker", "rmi", group + "/container-build-jib:0.1-SNAPSHOT")

File propertiesFile =
        System.getProperty("user.dir").endsWith("maven-invoker-way")
                ? Paths.get("target", "it", "container-build-jib", "target", "quarkus-artifact.properties").toFile()
                : Paths.get("integration-tests", "container-image", "maven-invoker-way", "target", "it", "container-build-jib", "target", "quarkus-artifact.properties").toFile()
Properties properties = new Properties()
propertiesFile.withInputStream {
    properties.load(it)
}



assert properties.type == 'jar-container'
assert properties."metadata.container-image" == group + "/container-build-jib:0.1-SNAPSHOT"
