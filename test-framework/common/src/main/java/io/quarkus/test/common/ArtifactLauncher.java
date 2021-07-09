package io.quarkus.test.common;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface ArtifactLauncher<T extends ArtifactLauncher.InitContext> extends Closeable {

    void init(T t);

    void start() throws IOException;

    void includeAsSysProps(Map<String, String> systemProps);

    boolean listensOnSsl();

    interface InitContext {

        int httpPort();

        int httpsPort();

        Duration waitTime();

        String testProfile();

        List<String> argLine();
    }
}
