package io.quarkus.devui.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.logging.Log;
import io.smallrye.common.os.OS;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class ReportAnIssueJsonRPCService {

    public JsonObject reportBug() {
        URLBuilder urlBuilder = new URLBuilder(
                "https://github.com/quarkusio/quarkus/issues/new?assignees=&labels=kind%2Fbug&template=bug_report.yml");
        gatherInfo(urlBuilder);
        return new JsonObject(
                Map.of("url", urlBuilder.toString()));
    }

    String run(String... command) {
        Process process = null;
        StringBuilder responseBuilder = new StringBuilder();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder().command(command);

            process = processBuilder.start();
            try (InputStream inputStream = process.getInputStream()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        responseBuilder.append(line);
                    }
                    safeWaitFor(process);
                }
            } catch (Throwable t) {
                safeWaitFor(process);
                throw t;
            }
        } catch (Exception e) {
            Log.warn("Error while running command: " + Arrays.toString(command), e);
            return "";
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
        return responseBuilder.toString();
    }

    private static void safeWaitFor(Process process) {
        boolean intr = false;
        try {
            for (;;)
                try {
                    process.waitFor();
                    return;
                } catch (InterruptedException ex) {
                    intr = true;
                }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void gatherInfo(URLBuilder builder) {
        builder.addQueryParameter("java_version", System.getProperty("java.version"))
                .addQueryParameter("quarkus_version",
                        Objects.toString(getClass().getPackage().getImplementationVersion(), "999-SNAPSHOT"));

        if (OS.WINDOWS.isCurrent()) {
            builder.addQueryParameter("uname", run("cmd.exe", "/C", "ver"));
            if (new File("mvnw.cmd").exists()) {
                builder.addQueryParameter("build_tool", run("./mvnw.cmd", "--version"));
            }
            if (new File("gradlew.bat").exists()) {
                builder.addQueryParameter("build_tool", run("./gradlew.bat", "--version"));
            }
        } else {
            builder.addQueryParameter("uname", run("uname", "-a"));
            if (new File("mvnw").exists()) {
                builder.addQueryParameter("build_tool", run("./mvnw", "--version"));
            }
            if (new File("gradlew").exists()) {
                builder.addQueryParameter("build_tool", run("./gradlew", "--version"));
            }
        }
    }

    static class URLBuilder {
        private final StringBuilder url;

        public URLBuilder(String url) {
            this.url = new StringBuilder(url);
        }

        public URLBuilder addQueryParameter(String key, String value) {
            if (this.url.indexOf("?") == -1) {
                this.url.append("?");
            } else {
                this.url.append("&");
            }

            this.url.append(encodeToUTF(key).replaceAll("[+]", "%20")).append("=")
                    .append(encodeToUTF(value.replaceAll(System.lineSeparator(), " ")).replaceAll("[+]", "%20"));
            return this;
        }

        static String encodeToUTF(String value) {
            try {
                return URLEncoder.encode(value, StandardCharsets.UTF_8.displayName());
            } catch (UnsupportedEncodingException var2) {
                throw new RuntimeException(var2);
            }
        }

        public String toString() {
            return this.url.toString();
        }
    }

}
