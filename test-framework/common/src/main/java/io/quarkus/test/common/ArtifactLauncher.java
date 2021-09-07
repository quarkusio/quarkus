package io.quarkus.test.common;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface ArtifactLauncher<T extends ArtifactLauncher.InitContext> extends Closeable {

    void init(T t);

    void start() throws IOException;

    LaunchResult runToCompletion(String[] args);

    void includeAsSysProps(Map<String, String> systemProps);

    boolean listensOnSsl();

    interface InitContext {

        int httpPort();

        int httpsPort();

        Duration waitTime();

        String testProfile();

        List<String> argLine();

        ArtifactLauncher.InitContext.DevServicesLaunchResult getDevServicesLaunchResult();

        interface DevServicesLaunchResult extends AutoCloseable {

            Map<String, String> properties();

            String networkId();

            void close();
        }
    }

    class LaunchResult {
        final int statusCode;
        final byte[] output;
        final byte[] stderror;

        public LaunchResult(int statusCode, byte[] output, byte[] stderror) {
            this.statusCode = statusCode;
            this.output = output;
            this.stderror = stderror;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public byte[] getOutput() {
            return output;
        }

        public byte[] getStderror() {
            return stderror;
        }
    }
}
