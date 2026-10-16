import static io.restassured.RestAssured.get
import static org.hamcrest.Matchers.containsString

try {
    execWithSystemLogging("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

get("http://localhost:5000/v2/_catalog")
        .then()
        .body(containsString("container-image-push"))

assert execWithSystemLogging("docker", "stop", "registry")

boolean execWithSystemLogging(String... command) {
    def process = new ProcessBuilder(command).inheritIO().start()
    return process.waitFor() == 0
}
