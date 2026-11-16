package io.quarkus.vertx.http.crac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

@EnabledIf("isCracEnabledJvm")
public class CracCheckpointRestoreTest {
    static final String CR_PATH = Path.of(System.getProperty("java.io.tmpdir"), "cr").toString();

    public static boolean isCracEnabledJvm() {
        return classExists("javax.crac.Resource") || classExists("jdk.crac.Resource");
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            // ignored
        }
        return false;
    }

    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> {
                jar.addClass(CracRoute.class);
                jar.addClass(MgmtRoute.class);
            })
            .setJVMArgs(List.of("-XX:CRaCCheckpointTo=" + CR_PATH))
            .overrideConfigKey("quarkus.package.jar.crac.enabled", "true")
            .overrideConfigKey("quarkus.management.enabled", "true");

    @Test
    public void testCheckpointRestore() throws IOException, InterruptedException {
        var checkpointedProcess = TEST.start();
        RestAssured.get("/crac").then().statusCode(200).body(is("true"));
        RestAssured.get("http://localhost:9000/ping").then().statusCode(200);

        Process jcmdProcess = new ProcessBuilder(command("jcmd"),
                String.valueOf(checkpointedProcess.pid()), "JDK.checkpoint").inheritIO().start();
        assertThat(jcmdProcess.waitFor(1, TimeUnit.MINUTES)).isTrue();
        assertThat(jcmdProcess.exitValue()).isZero();
        assertThat(checkpointedProcess.waitFor(1, TimeUnit.MINUTES)).isTrue();
        assertThat(checkpointedProcess.exitValue()).isEqualTo(137);

        Process restoredProcess = new ProcessBuilder(command("java"), "-XX:CRaCRestoreFrom=" + CR_PATH)
                .inheritIO().start();
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .ignoreException(ConnectException.class)
                .untilAsserted(() -> RestAssured.get("/crac").then().statusCode(200));
        RestAssured.get("http://localhost:9000/ping").then().statusCode(200);
        restoredProcess.destroy();
        if (!restoredProcess.waitFor(1, TimeUnit.MINUTES)) {
            restoredProcess.destroyForcibly();
        }
    }

    private static String command(String jcmd) {
        return Path.of(System.getProperty("java.home"), "bin", jcmd).toString();
    }

    @AfterAll
    public static void cleanup() {
        IoUtils.recursiveDelete(Path.of(CR_PATH));
    }
}
