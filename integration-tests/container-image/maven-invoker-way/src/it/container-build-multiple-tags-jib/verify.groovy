import io.quarkus.deployment.util.ExecUtil

try {
    ExecUtil.execWithSystemLogging("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

String group = System.getProperty("user.name")
assert ExecUtil.execWithSystemLogging("docker", "images", group + "/container-build-multiple-tags-jib")
assert ExecUtil.execWithSystemLogging("docker", "rmi", group + "/container-build-multiple-tags-jib:foo")
assert ExecUtil.execWithSystemLogging("docker", "rmi", group + "/container-build-multiple-tags-jib:bar")
assert ExecUtil.execWithSystemLogging("docker", "rmi", group + "/container-build-multiple-tags-jib:baz")
