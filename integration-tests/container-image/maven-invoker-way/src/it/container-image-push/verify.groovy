import io.quarkus.deployment.util.ExecUtil

import static io.restassured.RestAssured.get
import static org.hamcrest.Matchers.containsString

ExecUtil.useSystemLogging() //prevents stack overflow issues
try {
    ExecUtil.exec("docker", "version", "--format", "'{{.Server.Version}}'")
} catch (Exception ignored) {
    println "Docker not found"
    return
}

get("http://localhost:5000/v2/_catalog")
        .then()
        .body(containsString("container-image-push"))

assert ExecUtil.exec("docker", "stop", "registry")
