package io.quarkus.jbang;

import static io.quarkus.bootstrap.util.PropertyUtils.isWindows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;

class RunJBangTest {

    private static final String LATEST_DOWNLOAD_JBANG_ZIP = "https://www.jbang.dev/releases/latest/download/jbang.zip";
    private static Path DEVMODE_SCRIPT;
    private static Path JBANG_DIR = null;

    /**
     * Downloads latest jbang in static dir and setup JBANG_DIR to be 100% isolated
     * from whatever jbang could otherwise be installed on the system.
     */
    @BeforeAll
    static void downloadLatestJBang(@TempDir Path tempdir) throws IOException {
        InputStream in = new URL(LATEST_DOWNLOAD_JBANG_ZIP).openStream();
        JBANG_DIR = tempdir.resolve(".jbang");
        Path zipfile = tempdir.resolve("jbang.zip");
        Files.copy(in, zipfile, StandardCopyOption.REPLACE_EXISTING);
        TestHelper.unzip(zipfile, JBANG_DIR, true, null);

        //copy over files to allow edit
        DEVMODE_SCRIPT = tempdir.resolve("devmode.java");
        Files.copy(new File("target/test-classes/devmode.java").toPath(), DEVMODE_SCRIPT, StandardCopyOption.REPLACE_EXISTING);
    }

    private ProcessExecutor getJBangExecutor(String... args) {
        String cmd;
        if (isWindows()) {
            cmd = "bin/jbang.cmd";
        } else {
            cmd = "bin/jbang";
        }

        List<String> realargs = new ArrayList<>();
        realargs.add(JBANG_DIR.resolve(cmd).toString());
        realargs.addAll(Arrays.asList(args));

        return new ProcessExecutor().command(realargs)
                .environment("JBANG_DIR", JBANG_DIR.toAbsolutePath().toString())
                .redirectErrorStream(true).readOutput(true);
    }

    /**
     * just a sanity check that jbang is behaving as we expect with respect to config
     **/
    @Test
    void testRunJBangWithProperDir() throws IOException, InterruptedException, TimeoutException {

        String result = getJBangExecutor("version", "--verbose")
                .exitValue(0)
                .execute().outputUTF8();
        assertThat(result).contains(JBANG_DIR.toAbsolutePath().toString());
    }

    @Test
    void testRunJBangInDevMode() throws Throwable {

        final CompletableFuture<Integer> listening = new CompletableFuture<>();
        final CountDownLatch devmode = new CountDownLatch(1);
        final CountDownLatch replaced = new CountDownLatch(1);

        StartedProcess exec = getJBangExecutor("-Dquarkus.dev", "-Dq.v=999-SNAPSHOT",
                "-Dquarkus.http.port=0",
                DEVMODE_SCRIPT.toAbsolutePath().toString())
                        .exitValue(77) // 77 - means we explicitly killed it. Anything else something went wrong
                        .redirectOutput(new LogOutputStream() {
                            @Override
                            protected void processLine(String line) {
                                System.out.println(line);
                                if (line.contains("Listening on:")) {
                                    Pattern p = Pattern.compile("http://localhost:(\\d+)");
                                    Matcher matcher = p.matcher(line);
                                    if (!matcher.find()) {
                                        listening.completeExceptionally(
                                                new RuntimeException("Unable to determine port: " + line));
                                        return;
                                    }
                                    listening.complete(Integer.parseInt(matcher.group(1)));
                                } else if (line.contains("Profile dev activated")) {
                                    devmode.countDown();
                                } else if (line.contains("Changed source files detected, recompiling devmode.java")) {
                                    replaced.countDown();
                                }
                            }
                        })
                        .start();

        try {
            String base = "http://localhost:" + listening.get(10, TimeUnit.SECONDS);

            //check quarkus is running and responding
            String result = TestHelper.performRequest(base + "/hello", 200);
            assertThat(result).isEqualTo("Hello from Quarkus with jbang.dev");

            //if listening then devmode should also already be there
            assertThatNoException().as("dev mode not activated while running jbang")
                    .isThrownBy(() -> devmode.await(5, TimeUnit.MILLISECONDS));

            String script = Files.readString(DEVMODE_SCRIPT);

            script = script.replace("return \"Hello from Quarkus with jbang.dev\";", "return \"replaced\";");

            Files.writeString(DEVMODE_SCRIPT, script);

            assertThat(Files.readString(DEVMODE_SCRIPT)).contains("return \"replaced\";");

            //check devmode had an affect
            result = TestHelper.performRequest(base + "/hello", 200);

            assertThatNoException().as("code not replaced by updates")
                    .isThrownBy(() -> replaced.await(2, TimeUnit.MILLISECONDS));

            assertThat(result).isEqualTo("replaced");

            //kill quarkus process
            result = TestHelper.performRequest(base + "/hello/kill", 200);
            assertThat(result).isEqualTo("KILLED");
            exec.getFuture().get(10, TimeUnit.SECONDS);
            Assertions.assertEquals(77, exec.getProcess().exitValue());
        } catch (Throwable t) {
            exec.getProcess().destroyForcibly();
            throw t;
        }
    }

}
