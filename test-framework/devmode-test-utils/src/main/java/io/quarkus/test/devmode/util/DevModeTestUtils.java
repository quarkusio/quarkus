package io.quarkus.test.devmode.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class DevModeTestUtils {

    public static void killDescendingProcesses() {
        // Warning: Do not try to evaluate ProcessHandle.Info.arguments() or .commandLine() as those are always empty on Windows:
        // https://bugs.openjdk.java.net/browse/JDK-8176725
        ProcessHandle.current().descendants()
                // destroy younger descendants first
                .sorted((ph1, ph2) -> ph2.info().startInstant().orElse(Instant.EPOCH)
                        .compareTo(ph1.info().startInstant().orElse(Instant.EPOCH)))
                .forEach(ProcessHandle::destroy);
    }

    public static void filter(File input, Map<String, String> variables) throws IOException {
        assertThat(input).isFile();
        String data = FileUtils.readFileToString(input, "UTF-8");
        for (Map.Entry<String, String> token : variables.entrySet()) {
            String value = String.valueOf(token.getValue());
            data = data.replace(token.getKey(), value);
        }
        FileUtils.write(input, data, "UTF-8");
    }

    public static void awaitUntilServerDown() {
        await().atMost(1, TimeUnit.MINUTES).until(() -> {
            try {
                get(); // Ignore result on purpose
                return false;
            } catch (Exception e) {
                return true;
            }
        });
    }

    public static String getHttpResponse() {
        return getHttpResponse(() -> null);
    }

    public static String getHttpResponse(Supplier<String> brokenReason) {
        AtomicReference<String> resp = new AtomicReference<>();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                //Allow for a long maximum time as the first hit to a build might require to download dependencies from Maven repositories;
                //some, such as org.jetbrains.kotlin:kotlin-compiler, are huge and will take more than a minute.
                .atMost(3, TimeUnit.MINUTES).until(() -> {
                    try {
                        String broken = brokenReason.get();
                        if (broken != null) {
                            //try and avoid waiting 3m
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

    public static String getHttpErrorResponse() {
        return getHttpErrorResponse(() -> null);
    }

    public static String getHttpErrorResponse(Supplier<String> brokenReason) {
        AtomicReference<String> resp = new AtomicReference<>();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                //Allow for a long maximum time as the first hit to a build might require to download dependencies from Maven repositories;
                //some, such as org.jetbrains.kotlin:kotlin-compiler, are huge and will take more than a minute.
                .atMost(20, TimeUnit.MINUTES).until(() -> {
                    try {
                        String broken = brokenReason.get();
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

    public static String getHttpResponse(String path) {
        return getHttpResponse(path, false);
    }

    public static String getHttpResponse(String path, Supplier<String> brokenReason) {
        return getHttpResponse(path, false, brokenReason);
    }

    public static String getHttpResponse(String path, boolean allowError) {
        return getHttpResponse(path, allowError, () -> null);
    }

    public static String getHttpResponse(String path, boolean allowError, Supplier<String> brokenReason) {
        return getHttpResponse(path, allowError, brokenReason, 1, TimeUnit.MINUTES);
    }

    public static String getHttpResponse(String path, boolean allowError, Supplier<String> brokenReason, long timeout,
            TimeUnit tu) {
        AtomicReference<String> resp = new AtomicReference<>();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(timeout, tu).until(() -> {
                    String broken = brokenReason.get();
                    if (broken != null) {
                        resp.set("BROKEN: " + broken);
                        return true;
                    }
                    try {
                        URL url = new URL("http://localhost:8080" + ((path.startsWith("/") ? path : "/" + path)));
                        String content;
                        if (!allowError) {
                            content = IOUtils.toString(url, StandardCharsets.UTF_8);
                        } else {
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            // the default Accept header used by HttpURLConnection is not compatible with RESTEasy negotiation as it uses q=.8
                            conn.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
                            if (conn.getResponseCode() >= 400) {
                                content = IOUtils.toString(conn.getErrorStream(), StandardCharsets.UTF_8);
                            } else {
                                content = IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);
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

    public static boolean getHttpResponse(String path, int expectedStatus) {
        return getHttpResponse(path, expectedStatus, 5, TimeUnit.MINUTES);
    }

    public static boolean getHttpResponse(String path, int expectedStatus, long timeout, TimeUnit tu) {
        AtomicBoolean code = new AtomicBoolean();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(timeout, tu).until(() -> {
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
    public static boolean getStrictHttpResponse(String path, int expectedStatus) {
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
        return get("http://localhost:8080");
    }

    public static String get(String urlStr) throws IOException {
        return IOUtils.toString(new URL(urlStr), StandardCharsets.UTF_8);
    }

    public static boolean isCode(String path, int code) {
        try {
            URL url = new URL("http://localhost:8080" + ((path.startsWith("/") ? path : "/" + path)));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // the default Accept header used by HttpURLConnection is not compatible with
            // RESTEasy negotiation as it uses q=.2
            connection.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
            return connection.getResponseCode() == code;
        } catch (Exception e) {
            return false;
        }
    }
}
