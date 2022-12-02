import io.quarkus.deployment.util.ExecUtil

try {
    ExecUtil.exec("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

String group = System.getProperty("user.name")
assert ExecUtil.exec("docker", "images", group + "/container-build-multiple-tags-docker")
assert ExecUtil.exec("docker", "rmi", group + "/container-build-multiple-tags-docker:foo")
assert ExecUtil.exec("docker", "rmi", group + "/container-build-multiple-tags-docker:bar")
assert ExecUtil.exec("docker", "rmi", group + "/container-build-multiple-tags-docker:baz")
