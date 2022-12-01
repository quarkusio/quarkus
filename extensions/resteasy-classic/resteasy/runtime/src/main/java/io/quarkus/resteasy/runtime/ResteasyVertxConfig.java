package io.quarkus.resteasy.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "resteasy.vertx")
public class ResteasyVertxConfig {

    /**
     * The size of the output stream response buffer. If a response is larger than this and no content-length
     * is provided then the request will be chunked.
     *
     * Larger values may give slight performance increases for large responses, at the expense of more memory usage.
     */
    @ConfigItem(defaultValue = "8191")
    public int responseBufferSize;

}
