package io.quarkus.websockets.client.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class WebsocketConfig {

    /**
     * The maximum amount of data that can be sent in a single frame.
     *
     * Messages larger than this must be broken up into continuation frames.
     */
    @ConfigItem(defaultValue = "65536")
    public int maxFrameSize;

    /**
     * If the websocket methods should be run in a worker thread. This allows them to run
     * blocking tasks, however it will not be as fast as running directly in the IO thread.
     */
    @ConfigItem(defaultValue = "false")
    public boolean dispatchToWorker;
}
