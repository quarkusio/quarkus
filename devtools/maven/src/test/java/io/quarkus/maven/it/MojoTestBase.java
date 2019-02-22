package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.shared.utils.StringUtils;
import org.junit.jupiter.api.BeforeAll;

import com.google.common.collect.ImmutableMap;

import io.quarkus.maven.utilities.MojoUtils;

public class MojoTestBase {
    private static ImmutableMap<String, String> VARIABLES;

    @BeforeAll
    public static void init() {
        VARIABLES = ImmutableMap.of(
                "@project.groupId@", MojoUtils.getPluginGroupId(),
                "@project.artifactId@", MojoUtils.getPluginArtifactId(),
                "@project.version@", MojoUtils.getPluginVersion());
    }

    static File initEmptyProject(String name) {
        File tc = new File("target/test-classes/" + name);
        if (tc.isDirectory()) {
            boolean delete = tc.delete();
            Logger.getLogger(MojoTestBase.class.getName())
                    .log(Level.FINE, "test-classes deleted? " + delete);
        }
        boolean mkdirs = tc.mkdirs();
        Logger.getLogger(MojoTestBase.class.getName())
                .log(Level.FINE, "test-classes created? " + mkdirs);
        return tc;
    }

    public static File initProject(String name, String output) {
        File tc = new File("target/test-classes");
        if (!tc.isDirectory()) {
            boolean mkdirs = tc.mkdirs();
            Logger.getLogger(MojoTestBase.class.getName())
                    .log(Level.FINE, "test-classes created? " + mkdirs);
        }

        File in = new File("src/test/resources", name);
        if (!in.isDirectory()) {
            throw new RuntimeException("Cannot find directory: " + in.getAbsolutePath());
        }

        File out = new File(tc, output);
        if (out.isDirectory()) {
            FileUtils.deleteQuietly(out);
        }
        boolean mkdirs = out.mkdirs();
        Logger.getLogger(MojoTestBase.class.getName())
                .log(Level.FINE, out.getAbsolutePath() + " created? " + mkdirs);
        try {
            org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(in, out);
        } catch (IOException e) {
            throw new RuntimeException("Cannot copy project resources", e);
        }

        File pom = new File(out, "pom.xml");
        try {
            filter(pom, VARIABLES);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return out;
    }

    public static void installPluginToLocalRepository(File local) {
        File repo = new File(local, MojoUtils.getPluginGroupId().replace(".", "/") + "/"
                + MojoUtils.getPluginArtifactId() + "/" + MojoUtils.getPluginVersion());
        if (!repo.isDirectory()) {
            boolean mkdirs = repo.mkdirs();
            Logger.getLogger(MojoTestBase.class.getName())
                    .log(Level.FINE, repo.getAbsolutePath() + " created? " + mkdirs);
        }

        File plugin = new File("target", MojoUtils.getPluginArtifactId() + "-" + MojoUtils.getPluginVersion() + ".jar");
        if (!plugin.isFile()) {
            File[] files = new File("target").listFiles(
                    file -> file.getName().startsWith(MojoUtils.getPluginArtifactId()) && file.getName().endsWith(".jar"));
            if (files != null && files.length != 0) {
                plugin = files[0];
            }
        }

        try {
            FileUtils.copyFileToDirectory(plugin, repo);
            String installedPomName = MojoUtils.getPluginArtifactId() + "-" + MojoUtils.getPluginVersion() + ".pom";
            FileUtils.copyFile(new File("pom.xml"), new File(repo, installedPomName));
        } catch (IOException e) {
            throw new RuntimeException("Cannot copy the plugin jar, or the pom file, to the local repository", e);
        }
    }

    static void filter(File input, Map<String, String> variables) throws IOException {
        assertThat(input).isFile();
        String data = FileUtils.readFileToString(input, "UTF-8");

        for (Map.Entry<String, String> token : variables.entrySet()) {
            String value = String.valueOf(token.getValue());
            data = StringUtils.replace(data, token.getKey(), value);
        }
        FileUtils.write(input, data, "UTF-8");
    }

    Map<String, String> getEnv() {
        String opts = System.getProperty("mavenOpts");
        Map<String, String> env = new HashMap<>();
        if (opts != null) {
            env.put("MAVEN_OPTS", opts);
        }
        return env;
    }

    static void awaitUntilServerDown() {
        await().atMost(1, TimeUnit.MINUTES).until(() -> {
            try {
                get(); // Ignore result on purpose
                return false;
            } catch (Exception e) {
                return true;
            }
        });
    }

    static String getHttpResponse() {
        AtomicReference<String> resp = new AtomicReference<>();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> {
                    try {
                        String content = get();
                        resp.set(content);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
        return resp.get();
    }

    static String getHttpResponse(String path) {
        AtomicReference<String> resp = new AtomicReference<>();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> {
                    try {
                        URL url = new URL("http://localhost:8080" + ((path.startsWith("/") ? path : "/" + path)));
                        String content = IOUtils.toString(url, "UTF-8");
                        resp.set(content);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
        return resp.get();
    }

    static boolean getHttpResponse(String path, int expectedStatus) {
        AtomicBoolean code = new AtomicBoolean();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.MINUTES).until(() -> {
                    try {
                        URL url = new URL("http://localhost:8080" + ((path.startsWith("/") ? path : "/" + path)));
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        if (connection.getResponseCode() == expectedStatus) {
                            code.set(true);
                            return true;
                        }
                        return false;
                    } catch (Exception e) {
                        return false;
                    }
                });
        return code.get();
    }

    public static String get() throws IOException {
        URL url = new URL("http://localhost:8080");
        return IOUtils.toString(url, "UTF-8");
    }

    protected void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
