import io.quarkus.deployment.util.ExecUtil

import java.util.concurrent.ThreadLocalRandom

try {
    ExecUtil.execWithSystemLogging("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    return
}

String image = "${System.getProperty("user.name")}/container-build-jib-appcds:0.1-SNAPSHOT"
assert ExecUtil.execWithSystemLogging("docker", "images", image)

String containerName = "container-build-jib-appcds-" + ThreadLocalRandom.current().nextInt(10000)
int maxTimesToCheck = 10
int i = 0
int hostPort = 12345
assert ExecUtil.execWithSystemLogging("docker", "run", "-d", "-p", "$hostPort:8080", "--name", containerName, image)

while (true) {
    try {
        def response = "http://localhost:$hostPort/hello".toURL().text
        assert response == "hello"
        break
    } catch (IOException e) {
        try {
            Thread.sleep(2000)
        } catch (InterruptedException ignored) {
        }
        if ((i++) >= maxTimesToCheck) {
            throw new RuntimeException("Unable to determine if container is running", e)
        }
    }
}
assert ExecUtil.execWithSystemLogging("docker", "stop", containerName)
assert ExecUtil.execWithSystemLogging("docker", "rm", containerName)
assert ExecUtil.execWithSystemLogging("docker", "rmi", image)
