package io.quarkus.logging;

import static java.net.http.HttpResponse.BodyHandlers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.BasicLogger;

public class GenerateLog {
    private static final String PACKAGE_AND_IMPORT = "" +
            "package " + Log.class.getPackageName() + ";\n" +
            "\n" +
            "import org.jboss.logging.Logger;";
    private static final String CLASS_HEADER = "" +
            "/**\n" +
            " * Copy of {@link org.jboss.logging.BasicLogger}.\n" +
            " * Invocations of all {@code static} methods of this class are, during build time, replaced by invocations\n" +
            " * of the same methods on a generated instance of {@link Logger}.\n" +
            " */\n" +
            "public final class Log";
    private static final String LOG_METHOD_BODY = ") {\n" +
            "        throw fail();\n" +
            "    }";
    private static final String FAIL_METHOD = "" +
            "\n" +
            "    private static UnsupportedOperationException fail() {\n" +
            "        return new UnsupportedOperationException(\"Using \" + Log.class.getName()\n" +
            "                + \" is only possible with Quarkus bytecode transformation\");\n" +
            "    }\n";

    public static void main(String[] args) throws Exception {
        String source = BasicLogger.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        Matcher matcher = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.Final").matcher(source);
        if (matcher.find()) {
            String version = matcher.group();
            String url = "https://raw.githubusercontent.com/jboss-logging/jboss-logging/" + version
                    + "/src/main/java/org/jboss/logging/BasicLogger.java";
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(new URI(url)).build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                generateLogClass(response.body());
            } else {
                throw new Exception("Failed fetching " + url);
            }
        } else {
            throw new Exception("Couldn't find JBoss Logging version in " + source);
        }
    }

    private static void generateLogClass(String basicLogger) {
        String quarkusLog = basicLogger
                .replaceFirst("(?s).*?package org.jboss.logging;", PACKAGE_AND_IMPORT)
                .replaceFirst("(?s)/\\*\\*.*?public interface BasicLogger", CLASS_HEADER)
                .replaceAll("void", "public static void")
                .replaceAll("boolean", "public static boolean")
                .replaceAll("\\);", LOG_METHOD_BODY)
                .replaceFirst("}\\s*$", FAIL_METHOD + "}\n");

        System.out.println(quarkusLog);
    }
}
