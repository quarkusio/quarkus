package io.quarkus.websockets.client.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.websocket")
public interface WebsocketConfig {

    /**
     * The maximum amount of data that can be sent in a single frame.
     *
     * Messages larger than this must be broken up into continuation frames.
     */
    @WithDefault("65536")
    int maxFrameSize();

    /**
     * If the websocket methods should be run in a worker thread. This allows them to run
     * blocking tasks, however it will not be as fast as running directly in the IO thread.
     */
    @WithDefault("false")
    boolean dispatchToWorker();
}
