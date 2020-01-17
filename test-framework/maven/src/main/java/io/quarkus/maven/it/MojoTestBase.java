package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.utils.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class MojoTestBase {

    public static Invoker initInvoker(File root) {
        Invoker invoker = new DefaultInvoker();
        invoker.setWorkingDirectory(root);
        String repo = System.getProperty("maven.repo");
        if (repo == null) {
            repo = new File(System.getProperty("user.home"), ".m2/repository").getAbsolutePath();
        }
        invoker.setLocalRepositoryDirectory(new File(repo));
        return invoker;
    }

    public static File initEmptyProject(String name) {
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

    public static File initProject(String name) {
        return initProject(name, name);
    }

    public static File getTargetDir(String name) {
        return new File("target/test-classes/" + name);
    }

    public static File initProject(String name, String output) {
        File tc = new File("target/test-classes");
        if (!tc.isDirectory()) {
            boolean mkdirs = tc.mkdirs();
            Logger.getLogger(MojoTestBase.class.getName())
                    .log(Level.FINE, "test-classes created? " + mkdirs);
        }

        File in = new File(tc, name);
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

        return out;
    }

    public static void filter(File input, Map<String, String> variables) throws IOException {
        assertThat(input).isFile();
        String data = FileUtils.readFileToString(input, "UTF-8");

        for (Map.Entry<String, String> token : variables.entrySet()) {
            String value = String.valueOf(token.getValue());
            data = StringUtils.replace(data, token.getKey(), value);
        }
        FileUtils.write(input, data, "UTF-8");
    }

    public Map<String, String> getEnv() {
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

    String getHttpResponse() {
        AtomicReference<String> resp = new AtomicReference<>();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                //Allow for a long maximum time as the first hit to a build might require to download dependencies from Maven repositories;
                //some, such as org.jetbrains.kotlin:kotlin-compiler, are huge and will take more than a minute.
                .atMost(20, TimeUnit.MINUTES).until(() -> {
                    try {
                        String broken = getBrokenReason();
                        if (broken != null) {
                            //try and avoid waiting 20m
                            resp.set("BROKEN: " + broken);
                            return true;
                        }
                        String content = get();
                        resp.set(content);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
        return resp.get();
    }

    String getHttpErrorResponse() {
        AtomicReference<String> resp = new AtomicReference<>();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                //Allow for a long maximum time as the first hit to a build might require to download dependencies from Maven repositories;
                //some, such as org.jetbrains.kotlin:kotlin-compiler, are huge and will take more than a minute.
                .atMost(20, TimeUnit.MINUTES).until(() -> {
                    try {
                        String broken = getBrokenReason();
                        if (broken != null) {
                            //try and avoid waiting 20m
                            resp.set("BROKEN: " + broken);
                            return true;
                        }
                        boolean content = getHttpResponse("/", 500);
                        return content;
                    } catch (Exception e) {
                        return false;
                    }
                });
        return resp.get();
    }

    protected String getBrokenReason() {
        return null;
    }

    public static String getHttpResponse(String path) {
        return getHttpResponse(path, false);
    }

    static String getHttpResponse(String path, boolean allowError) {
        AtomicReference<String> resp = new AtomicReference<>();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> {
                    try {
                        URL url = new URL("http://localhost:8080" + ((path.startsWith("/") ? path : "/" + path)));
                        String content;
                        if (!allowError) {
                            content = IOUtils.toString(url, "UTF-8");
                        } else {
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            // the default Accept header used by HttpURLConnection is not compatible with RESTEasy negotiation as it uses q=.8
                            conn.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
                            if (conn.getResponseCode() >= 400) {
                                content = IOUtils.toString(conn.getErrorStream(), "UTF-8");
                            } else {
                                content = IOUtils.toString(conn.getInputStream(), "UTF-8");
                            }
                        }
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
                        // the default Accept header used by HttpURLConnection is not compatible with RESTEasy negotiation as it uses q=.2
                        connection.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
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

    // will fail if it receives any http response except the expected one
    static boolean getStrictHttpResponse(String path, int expectedStatus) {
        AtomicBoolean code = new AtomicBoolean();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.MINUTES).until(() -> {
                    try {
                        URL url = new URL("http://localhost:8080" + ((path.startsWith("/") ? path : "/" + path)));
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        // the default Accept header used by HttpURLConnection is not compatible with RESTEasy negotiation as it uses q=.2
                        connection.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
                        code.set(connection.getResponseCode() == expectedStatus);
                        //complete no matter what the response code was
                        return true;
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

    public static void assertThatOutputWorksCorrectly(String logs) {
        assertThat(logs.isEmpty()).isFalse();
        String infoLogLevel = "INFO";
        assertThat(logs.contains(infoLogLevel)).isTrue();
        Predicate<String> datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2},\\d{3}").asPredicate();
        assertThat(datePattern.test(logs)).isTrue();
        assertThat(logs.contains("cdi, resteasy, servlet, undertow-websockets")).isTrue();
        assertThat(logs.contains("JBoss Threads version")).isFalse();
    }

    public static Model loadPom(File directory) {
        File pom = new File(directory, "pom.xml");
        assertThat(pom).isFile();
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(pom), StandardCharsets.UTF_8);) {
            return new MavenXpp3Reader().read(isr);
        } catch (IOException | XmlPullParserException e) {
            throw new IllegalArgumentException("Cannot read the pom.xml file", e);
        }
    }
}
