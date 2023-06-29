import io.quarkus.deployment.util.ExecUtil

try {
    ExecUtil.execWithSystemLogging("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

assert ExecUtil.execWithSystemLogging("docker", "run", "--rm", "-p", "5000:5000", "-d", "--name", "registry" ,"registry:2");
