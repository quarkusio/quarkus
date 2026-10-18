try {
    execWithSystemLogging("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

String group = System.getProperty("user.name")
assert execWithSystemLogging("docker", "images", group + "/container-build-docker")
assert execWithSystemLogging("docker", "rmi", group + "/container-build-docker:0.1-SNAPSHOT")

boolean execWithSystemLogging(String... command) {
    def process = new ProcessBuilder(command).inheritIO().start()
    return process.waitFor() == 0
}
