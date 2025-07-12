package io.quarkus.devui.runtime.reportissues;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.smallrye.common.os.OS;
import io.vertx.core.json.JsonObject;

@Dependent
public class ReportIssuesJsonRPCService {

    private static final Logger LOG = Logger.getLogger(ReportIssuesJsonRPCService.class);

    @Inject
    @ConfigProperty(name = "quarkus.devui.report-issues.url", defaultValue = "https://github.com/quarkusio/quarkus/issues/new?labels=kind%2Fbug&template=bug_report.yml")
    String reportURL;

    public JsonObject reportBug() {
        URLBuilder urlBuilder = new URLBuilder(reportURL);
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
            LOG.warn("Error while running command: " + Arrays.toString(command), e);
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
            if (isMavenProject()) {
                if (Files.exists(Path.of("mvnw.cmd"))) {
                    builder.addQueryParameter("build_tool", run("./mvnw.cmd", "--version"));
                } else {
                    // Use the system Maven
                    builder.addQueryParameter("build_tool", run("mvn", "--version"));
                }
            } else if (isGradleProject()) {
                if (Files.exists(Path.of("gradlew.bat"))) {
                    builder.addQueryParameter("build_tool", run("./gradlew.bat", "--version"));
                } else {
                    // Use the system Gradle
                    builder.addQueryParameter("build_tool", run("gradle", "--version"));
                }
            }
        } else {
            builder.addQueryParameter("uname", run("uname", "-a"));
            if (isMavenProject()) {
                if (Files.exists(Path.of("mvnw"))) {
                    builder.addQueryParameter("build_tool", run("./mvnw", "--version"));
                } else {
                    // Use the system Maven
                    builder.addQueryParameter("build_tool", run("mvn", "--version"));
                }
            } else if (isGradleProject()) {
                if (Files.exists(Path.of("gradlew"))) {
                    builder.addQueryParameter("build_tool", run("./gradlew", "--version"));
                } else {
                    // Use the system Gradle
                    builder.addQueryParameter("build_tool", run("gradle", "--version"));
                }
            }
        }
    }

    private static boolean isMavenProject() {
        return Files.exists(Path.of("pom.xml"));
    }

    private static boolean isGradleProject() {
        return Files.exists(Path.of("build.gradle")) || Files.exists(Path.of("build.gradle.kts"));
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
                    .append(encodeToUTF(value.replaceAll(System.lineSeparator(), "%20")).replaceAll("[+]", "%20"));
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
