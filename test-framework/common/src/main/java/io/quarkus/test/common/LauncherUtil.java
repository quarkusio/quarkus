package io.quarkus.test.common;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.smallrye.config.SmallRyeConfig;

final class LauncherUtil {

    private LauncherUtil() {
    }

    static Config installAndGetSomeConfig() {
        final SmallRyeConfig config = ConfigUtils.configBuilder(false).build();
        QuarkusConfigFactory.setConfig(config);
        final ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        try {
            final Config installed = cpr.getConfig();
            if (installed != config) {
                cpr.releaseConfig(installed);
            }
        } catch (IllegalStateException ignored) {
        }
        return config;
    }

    static int doStart(Process quarkusProcess, int httpPort, int httpsPort, long waitTime, Supplier<Boolean> startedSupplier) {
        PortCapturingProcessReader portCapturingProcessReader = null;
        if (httpPort == 0) {
            // when the port is 0, then the application starts on a random port and the only way for us to figure it out
            // is to capture the output
            portCapturingProcessReader = new PortCapturingProcessReader(quarkusProcess.getInputStream());
        }
        new Thread(portCapturingProcessReader != null ? portCapturingProcessReader
                : new ProcessReader(quarkusProcess.getInputStream())).start();
        new Thread(new ProcessReader(quarkusProcess.getErrorStream())).start();

        if (portCapturingProcessReader != null) {
            try {
                portCapturingProcessReader.awaitForPort();
            } catch (InterruptedException ignored) {

            }
            if (portCapturingProcessReader.getPort() == null) {
                quarkusProcess.destroy();
                throw new RuntimeException("Unable to determine actual running port as dynamic port was used");
            }

            waitForQuarkus(quarkusProcess, portCapturingProcessReader.getPort(), httpsPort, waitTime, startedSupplier);

            System.setProperty("quarkus.http.port", portCapturingProcessReader.getPort().toString()); //set the port as a system property in order to have it applied to Config
            System.setProperty("quarkus.http.test-port", portCapturingProcessReader.getPort().toString()); // needed for RestAssuredManager
            int capturedPort = portCapturingProcessReader.getPort();
            installAndGetSomeConfig(); // reinitialize the configuration to make sure the actual port is used
            System.setProperty("test.url", TestHTTPResourceManager.getUri());
            return capturedPort;
        } else {
            waitForQuarkus(quarkusProcess, httpPort, httpsPort, waitTime, startedSupplier);
            return httpPort;
        }
    }

    private static void waitForQuarkus(Process quarkusProcess, int httpPort, int httpsPort, long waitTime,
            Supplier<Boolean> startedSupplier) {
        long bailout = System.currentTimeMillis() + waitTime * 1000;

        while (System.currentTimeMillis() < bailout) {
            if (!quarkusProcess.isAlive()) {
                throw new RuntimeException("Failed to start target quarkus application, process has exited");
            }
            try {
                Thread.sleep(100);
                if (startedSupplier != null) {
                    if (startedSupplier.get()) {
                        return;
                    }
                }
                try {
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress("localhost", httpPort));
                        //SSL is bound after https
                        //we add a small delay to make sure SSL is available if installed
                        Thread.sleep(100);
                        return;
                    }
                } catch (Exception expected) {
                }
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress("localhost", httpsPort));
                    return;
                }
            } catch (Exception expected) {
            }
        }
        quarkusProcess.destroyForcibly();
        throw new RuntimeException("Unable to start target quarkus application " + waitTime + "s");
    }
}
