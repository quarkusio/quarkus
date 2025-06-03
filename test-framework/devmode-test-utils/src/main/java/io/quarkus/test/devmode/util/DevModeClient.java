package io.quarkus.test.devmode.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;

import io.smallrye.common.os.OS;

public class DevModeClient {

    private static final long DEFAULT_TIMEOUT = OS.current() == OS.WINDOWS ? 3L : 1L;

    private static final Logger LOG = Logger.getLogger(DevModeClient.class);

    static long getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    private final int port;

    public DevModeClient() {
        this(8080);
    }

    public DevModeClient(int port) {
        this.port = port;
    }

    public static List<ProcessHandle> killDescendingProcesses() {
        // Warning: Do not try to evaluate ProcessHandle.Info.arguments() or .commandLine() as those are always empty on Windows:
        // https://bugs.openjdk.java.net/browse/JDK-8176725
        //
        // Intentionally collecting the ProcessHandles before calling .destroy(), because it seemed that, at least on
        // Windows, not all processes were properly killed, leaving (some) processes around, causing following dev-mode
        // tests to time-out.
        List<ProcessHandle> childProcesses = ProcessHandle.current().descendants()
                // destroy younger descendants first
                .sorted((ph1, ph2) -> ph2.info().startInstant().orElse(Instant.EPOCH)
                        .compareTo(ph1.info().startInstant().orElse(Instant.EPOCH)))
                .collect(Collectors.toList());

        childProcesses.forEach(ProcessHandle::destroy);

        // Returning all child processes for callers that want to do a "kill -9"
        return childProcesses;
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

    public void awaitUntilServerDown() {
        await().atMost(DEFAULT_TIMEOUT, TimeUnit.MINUTES).until(() -> {
            try {
                get(); // Ignore result on purpose
                return false;
            } catch (Exception e) {
                return true;
            }
        });
    }

    public String getHttpResponse() {
        return getHttpResponse(() -> null);
    }

    public String getHttpResponse(Supplier<String> brokenReason) {
        AtomicReference<String> resp = new AtomicReference<>();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                //Allow for a long maximum time as the first hit to a build might require to download dependencies from Maven repositories;
                //some, such as org.jetbrains.kotlin:kotlin-compiler, are huge and will take more than a minute.
                .atMost(2L + DEFAULT_TIMEOUT, TimeUnit.MINUTES).until(() -> {
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

    public String getHttpErrorResponse() {
        return getHttpErrorResponse(() -> null);
    }

    public String getHttpErrorResponse(Supplier<String> brokenReason) {
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

    public String getHttpResponse(String path) {
        return getHttpResponse(path, false);
    }

    public String getHttpResponse(String path, Supplier<String> brokenReason) {
        return getHttpResponse(path, false, brokenReason);
    }

    public String getHttpResponse(String path, boolean allowError) {
        return getHttpResponse(path, allowError, () -> null);
    }

    public String getHttpResponse(String path, boolean allowError, Supplier<String> brokenReason) {
        return getHttpResponse(path, allowError, brokenReason, 1, TimeUnit.MINUTES);
    }

    public String getHttpResponse(String path, boolean allowError, Supplier<String> brokenReason, long timeout,
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
                        URL url = prepareUrl(path);

                        String content;
                        if (!allowError) {
                            content = IOUtils.toString(url, StandardCharsets.UTF_8);
                        } else {
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setDefaultUseCaches(false);
                            conn.setUseCaches(false);
                            // the default Accept header used by HttpURLConnection is not compatible with RESTEasy negotiation as it uses q=.8
                            conn.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
                            if (conn.getResponseCode() >= 400) {
                                content = IOUtils.toString(conn.getErrorStream(), StandardCharsets.UTF_8);
                            } else {
                                content = IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);
                            }
                            conn.disconnect();
                        }

                        resp.set(content);
                        return true;
                    } catch (Exception e) {
                        var sb = new StringBuilder();
                        sb.append("DevModeClient failed to accessed ").append(path)
                                .append(". It might be a normal testing behavior but logging the messages for information: ")
                                .append(e.getLocalizedMessage());
                        var cause = e.getCause();
                        while (cause != null) {
                            sb.append(": ").append(cause.getLocalizedMessage());
                            cause = cause.getCause();
                        }
                        LOG.error(sb);
                        return false;
                    }
                });
        return resp.get();
    }

    public boolean getHttpResponse(String path, int expectedStatus) {
        return getHttpResponse(path, expectedStatus, 5, TimeUnit.MINUTES);
    }

    public boolean getHttpResponse(String path, int expectedStatus, long timeout, TimeUnit tu) {
        AtomicBoolean code = new AtomicBoolean();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(timeout, tu).until(() -> {
                    try {
                        URL url = prepareUrl(path);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDefaultUseCaches(false);
                        connection.setUseCaches(false);
                        // the default Accept header used by HttpURLConnection is not compatible with RESTEasy negotiation as it uses q=.2
                        connection.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
                        if (connection.getResponseCode() == expectedStatus) {
                            code.set(true);
                            return true;
                        }
                        return false;
                    } catch (Exception e) {
                        LOG.error(
                                "An error occurred when DevModeClient accessed " + path
                                        + ". It might be a normal testing behavior but logging the exception for information",
                                e);
                        return false;
                    }
                });
        return code.get();
    }

    // will fail if it receives any http response except the expected one
    public boolean getStrictHttpResponse(String path, int expectedStatus) {
        AtomicBoolean code = new AtomicBoolean();
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.MINUTES).until(() -> {
                    try {
                        URL url = prepareUrl(path);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDefaultUseCaches(false);
                        connection.setUseCaches(false);
                        // the default Accept header used by HttpURLConnection is not compatible with RESTEasy negotiation as it uses q=.2
                        connection.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
                        code.set(connection.getResponseCode() == expectedStatus);
                        //complete no matter what the response code was
                        return true;
                    } catch (Exception e) {
                        LOG.error(
                                "An error occurred when DevModeClient accessed " + path
                                        + ". It might be a normal testing behavior but logging the exception for information",
                                e);
                        return false;
                    }
                });
        return code.get();
    }

    public String get() throws IOException {
        return get("http://localhost:" + port);
    }

    public String get(String urlStr) throws IOException {
        return IOUtils.toString(new URL(urlStr), StandardCharsets.UTF_8);
    }

    public boolean isCode(String path, int code) {
        try {
            URL url = prepareUrl(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDefaultUseCaches(false);
            connection.setUseCaches(false);
            // the default Accept header used by HttpURLConnection is not compatible with
            // RESTEasy negotiation as it uses q=.2
            connection.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
            return connection.getResponseCode() == code;
        } catch (Exception e) {
            return false;
        }
    }

    private URL prepareUrl(String path) throws MalformedURLException {
        String urlString = "http://localhost:" + port + (path.startsWith("/") ? path : "/" + path);
        if (urlString.contains("?")) {
            urlString += "&";
        } else {
            urlString += "?";
        }
        urlString += "_test_timestamp=" + System.currentTimeMillis();

        return new URL(urlString);
    }
}
