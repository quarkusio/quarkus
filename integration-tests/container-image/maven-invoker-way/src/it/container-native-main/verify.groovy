import io.quarkus.deployment.util.ExecUtil

try {
    ExecUtil.execWithSystemLogging("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

String group = System.getProperty("user.name")
assert ExecUtil.execWithSystemLogging("docker", "images", group + "/container-native-main")
assert ExecUtil.execWithSystemLogging("docker", "rmi", group + "/container-native-main:0.1-SNAPSHOT")
