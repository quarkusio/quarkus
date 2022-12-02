import io.quarkus.deployment.util.ExecUtil

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

try {
    ExecUtil.exec("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

assert ExecUtil.exec("docker", "images", "container-build-jib")
assert ExecUtil.exec("docker", "rmi", "container-build-jib:0.1-SNAPSHOT")


Path pathInIT = Paths.get("target", "it", "container-build-jib", "target")
Path target;
String userDir = System.getProperty("user.dir")
if (userDir.endsWith("maven-invoker-way")) {
    target = pathInIT
} else if (userDir.endsWith("integration-tests")) {
    target = Paths.get("container-image", "maven-invoker-way").resolve(pathInIT)
} else { // we are in the quarkus root
    target = Paths.get("integration-tests", "container-image", "maven-invoker-way").resolve(pathInIT)
}

assert Files.exists(target.toAbsolutePath())

File propertiesFile = target.resolve("quarkus-artifact.properties").toFile()
Properties properties = new Properties()
propertiesFile.withInputStream {
    properties.load(it)
}

assert properties.type == 'jar-container'
assert properties."metadata.container-image" == "container-build-jib:0.1-SNAPSHOT"


assert Files.exists(target.resolve("jib-image.digest"))
assert Files.exists(target.resolve("jib-image.id"))
