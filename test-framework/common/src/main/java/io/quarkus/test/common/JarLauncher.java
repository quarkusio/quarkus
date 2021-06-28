package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.installAndGetSomeConfig;
import static io.quarkus.test.common.LauncherUtil.updateConfigForPort;
import static io.quarkus.test.common.LauncherUtil.waitForCapturedListeningData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.eclipse.microprofile.config.Config;

import io.quarkus.test.common.http.TestHTTPResourceManager;

public class JarLauncher implements ArtifactLauncher {

    private static final int DEFAULT_PORT = 8081;
    private static final int DEFAULT_HTTPS_PORT = 8444;
    private static final long DEFAULT_JAR_WAIT_TIME = 60;

    private final Path jarPath;
    private final String profile;
    private final String argLine;
    private Process quarkusProcess;
    private final int httpPort;
    private final int httpsPort;
    private final long jarWaitTime;
    private final Map<String, String> systemProps = new HashMap<>();

    private boolean isSsl;

    private JarLauncher(Path jarPath, Config config) {
        this(jarPath,
                config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                config.getValue("quarkus.test.jar-wait-time", OptionalLong.class).orElse(DEFAULT_JAR_WAIT_TIME),
                config.getOptionalValue("quarkus.test.argLine", String.class).orElse(null),
                config.getOptionalValue("quarkus.test.native-image-profile", String.class)
                        .orElse(null));
    }

    public JarLauncher(Path jarPath) {
        this(jarPath, installAndGetSomeConfig());
    }

    public JarLauncher(Path jarPath, int httpPort, int httpsPort, long jarWaitTime, String argLine, String profile) {
        this.jarPath = jarPath;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.jarWaitTime = jarWaitTime;
        this.argLine = argLine;
        this.profile = profile;
    }

    public void start() throws IOException {

        System.setProperty("test.url", TestHTTPResourceManager.getUri());

        List<String> args = new ArrayList<>();
        args.add("java");
        if (argLine != null) {
            args.add(argLine);
        }
        args.add("-Dquarkus.http.port=" + httpPort);
        args.add("-Dquarkus.http.ssl-port=" + httpsPort);
        // this won't be correct when using the random port but it's really only used by us for the rest client tests
        // in the main module, since those tests hit the application itself
        args.add("-Dtest.url=" + TestHTTPResourceManager.getUri());
        Path logFile = PropertyTestUtil.getLogFilePath();
        args.add("-Dquarkus.log.file.path=" + logFile.toAbsolutePath().toString());
        args.add("-Dquarkus.log.file.enable=true");
        if (profile != null) {
            args.add("-Dquarkus.profile=" + profile);
        }
        for (Map.Entry<String, String> e : systemProps.entrySet()) {
            args.add("-D" + e.getKey() + "=" + e.getValue());
        }
        args.add("-jar");
        args.add(jarPath.toAbsolutePath().toString());

        System.out.println("Executing " + args);

        Files.deleteIfExists(logFile);
        Files.createDirectories(logFile.getParent());

        quarkusProcess = LauncherUtil.launchProcess(args);
        ListeningAddress result = waitForCapturedListeningData(quarkusProcess, logFile, jarWaitTime);
        updateConfigForPort(result.getPort());
        isSsl = result.isSsl();
    }

    public boolean listensOnSsl() {
        return isSsl;
    }

    public void addSystemProperties(Map<String, String> systemProps) {
        this.systemProps.putAll(systemProps);
    }

    @Override
    public void close() {
        quarkusProcess.destroy();
    }
}
