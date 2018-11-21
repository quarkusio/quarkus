package org.jboss.shamrock.undertow.runtime;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfiguredType;

@ConfiguredType
public class HttpConfig {

    /**
     * The HTTP port
     */
    @ConfigProperty(name = "port", defaultValue = "8080")
    public int port;

    /**
     * The HTTP host
     */
    @ConfigProperty(name = "host", defaultValue = "localhost")
    public String host;

    /**
     * The number of worker threads used for blocking tasks, this will be automatically set to a reasonable value
     * based on the number of CPU core if it is not provided
     */
    @ConfigProperty(name = "workerThreads")
    public Optional<Integer> workerThreads;

    /**
     * The number if IO threads used to perform IO. This will be automatically set to a reasonable value based on
     * the number of CPU cores if it is not provided
     */
    @ConfigProperty(name = "ioThreads")
    public Optional<Integer> ioThreads;

}
