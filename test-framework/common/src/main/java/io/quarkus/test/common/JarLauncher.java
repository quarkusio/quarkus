package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.installAndGetSomeConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private Process quarkusProcess;
    private int port;
    private final int httpsPort;
    private final long jarWaitTime;
    private final Map<String, String> systemProps = new HashMap<>();

    private JarLauncher(Path jarPath, Config config) {
        this(jarPath,
                config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                config.getValue("quarkus.test.jar-wait-time", OptionalLong.class).orElse(DEFAULT_JAR_WAIT_TIME),
                config.getOptionalValue("quarkus.test.native-image-profile", String.class)
                        .orElse(null));
    }

    public JarLauncher(Path jarPath) {
        this(jarPath, installAndGetSomeConfig());
    }

    public JarLauncher(Path jarPath, int port, int httpsPort, long jarWaitTime, String profile) {
        this.jarPath = jarPath;
        this.port = port;
        this.httpsPort = httpsPort;
        this.jarWaitTime = jarWaitTime;
        this.profile = profile;
    }

    public void start() throws IOException {

        System.setProperty("test.url", TestHTTPResourceManager.getUri());

        List<String> args = new ArrayList<>();
        args.add("java");
        args.add("-Dquarkus.http.port=" + port);
        args.add("-Dquarkus.http.ssl-port=" + httpsPort);
        // this won't be correct when using the random port but it's really only used by us for the rest client tests
        // in the main module, since those tests hit the application itself
        args.add("-Dtest.url=" + TestHTTPResourceManager.getUri());
        args.add("-Dquarkus.log.file.path=" + PropertyTestUtil.getLogFileLocation());
        if (profile != null) {
            args.add("-Dquarkus.profile=" + profile);
        }
        for (Map.Entry<String, String> e : systemProps.entrySet()) {
            args.add("-D" + e.getKey() + "=" + e.getValue());
        }
        args.add("-jar");
        args.add(jarPath.toAbsolutePath().toString());

        System.out.println("Executing " + args);

        quarkusProcess = Runtime.getRuntime().exec(args.toArray(new String[0]));
        port = LauncherUtil.doStart(quarkusProcess, port, httpsPort, jarWaitTime, null);
    }

    public boolean isDefaultSsl() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("localhost", port));
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    public void addSystemProperties(Map<String, String> systemProps) {
        this.systemProps.putAll(systemProps);
    }

    @Override
    public void close() {
        quarkusProcess.destroy();
    }
}
