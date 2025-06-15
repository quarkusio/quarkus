package io.quarkus.info.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.test.QuarkusUnitTest;

@EnabledOnOs(OS.LINUX) // as this test deals with temp files that are created manually, let's avoid dealing with other
                       // OSes
public class NoGitProjectInfoTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withEmptyApplication()
            .addBootstrapCustomizer(new Consumer<QuarkusBootstrap.Builder>() {
                @Override
                public void accept(QuarkusBootstrap.Builder builder) {
                    // needed to ensure that the workspace is not populated
                    builder.setLocalProjectDiscovery(false);
                    // needed to avoid bootstrap failing because of the previous property
                    builder.setDisableClasspathCache(true);
                    try {
                        // needed to ensure that the output directory is outside the source tree
                        Path tempDirectory = Files.createTempDirectory(Path.of("/tmp"), "info-test");
                        tempDirectory.toFile().deleteOnExit();
                        builder.setTargetDirectory(tempDirectory);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });

    @Test
    public void test() {
        when().get("/q/info").then().statusCode(200).body("os", is(notNullValue())).body("os.name", is(notNullValue()))
                .body("java", is(notNullValue())).body("java.version", is(notNullValue()))
                .body("build", is(notNullValue())).body("build.time", is(notNullValue())).body("git", is(nullValue()));

    }
}
