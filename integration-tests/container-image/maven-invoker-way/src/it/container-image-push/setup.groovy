import io.quarkus.deployment.util.ExecUtil

try {
    ExecUtil.exec("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

assert ExecUtil.exec("docker", "run", "--rm", "-p", "5000:5000", "-d", "--name", "registry" ,"registry:2");