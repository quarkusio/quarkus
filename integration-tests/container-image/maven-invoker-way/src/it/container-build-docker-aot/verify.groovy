import io.quarkus.deployment.util.ExecUtil

import java.nio.file.Files;
import java.io.File;
import java.util.concurrent.ThreadLocalRandom

// Check if Java version is less than 25
if (Runtime.version().feature() < 25) {
    println "Skipping test: Java feature version ${Runtime.version().feature()} is less than 25."
    return
}

try {
    ExecUtil.execWithSystemLogging("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    return
}

String baseImage = "${System.getProperty("user.name")}/container-build-docker-aot:0.1-SNAPSHOT"
String image = "$baseImage-aot"
assert ExecUtil.execWithSystemLogging("docker", "images", image)

String containerName = "container-build-docker-aot-" + ThreadLocalRandom.current().nextInt(10000)
int maxTimesToCheck = 10
int i = 0
int hostPort = 12345
final command = "docker run --rm -p $hostPort:8080 --name $containerName $image"
StringBuilder containerLogs = new StringBuilder()
final dockerRunProcess = command.execute()
dockerRunProcess.consumeProcessOutput(containerLogs, containerLogs)

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

final logs = containerLogs.toString()

try {
    dockerRunProcess.destroy()
    Thread.sleep(1000)
} catch(Exception ignored) {

}


try {
    assert logs.contains("-XX:AOTCache")
    assert !logs.contains("-Xlog:aot") // this is what is printed when there is an error
} finally {
    ExecUtil.execWithSystemLogging("docker", "rmi", "-f", image)
    ExecUtil.execWithSystemLogging("docker", "rmi", "-f", baseImage)
}






