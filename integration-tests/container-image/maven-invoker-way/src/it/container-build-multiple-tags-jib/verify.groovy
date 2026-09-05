try {
    execWithSystemLogging("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

String group = System.getProperty("user.name")
assert execWithSystemLogging("docker", "images", group + "/container-build-multiple-tags-jib")
assert execWithSystemLogging("docker", "rmi", group + "/container-build-multiple-tags-jib:foo")
assert execWithSystemLogging("docker", "rmi", group + "/container-build-multiple-tags-jib:bar")
assert execWithSystemLogging("docker", "rmi", group + "/container-build-multiple-tags-jib:baz")

boolean execWithSystemLogging(String... command) {
    def process = new ProcessBuilder(command).inheritIO().start()
    return process.waitFor() == 0
}
