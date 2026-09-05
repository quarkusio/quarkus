try {
    execWithSystemLogging("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

assert execWithSystemLogging("docker", "run", "--rm", "-p", "5000:5000", "-d", "--name", "registry" ,"registry:2");

boolean execWithSystemLogging(String... command) {
    def process = new ProcessBuilder(command).inheritIO().start()
    return process.waitFor() == 0
}
