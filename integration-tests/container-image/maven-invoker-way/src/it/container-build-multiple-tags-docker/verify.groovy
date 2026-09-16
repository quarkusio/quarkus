try {
    execWithSystemLogging("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

String group = System.getProperty("user.name")
assert execWithSystemLogging("docker", "images", group + "/container-build-multiple-tags-docker")
assert execWithSystemLogging("docker", "rmi", group + "/container-build-multiple-tags-docker:foo")
assert execWithSystemLogging("docker", "rmi", group + "/container-build-multiple-tags-docker:bar")
assert execWithSystemLogging("docker", "rmi", group + "/container-build-multiple-tags-docker:baz")

boolean execWithSystemLogging(String... command) {
    def process = new ProcessBuilder(command).inheritIO().start()
    return process.waitFor() == 0
}
