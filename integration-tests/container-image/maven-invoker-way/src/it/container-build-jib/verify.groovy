import io.quarkus.deployment.util.ExecUtil

try {
    ExecUtil.exec("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

String group = System.getProperty("user.name")
assert ExecUtil.exec("docker", "images", group + "/container-build-jib")
assert ExecUtil.exec("docker", "rmi", group + "/container-build-jib:0.1-SNAPSHOT")
