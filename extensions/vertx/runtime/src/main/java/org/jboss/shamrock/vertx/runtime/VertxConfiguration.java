package org.jboss.shamrock.vertx.runtime;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfigGroup;

import java.util.Optional;

@ConfigGroup
public class VertxConfiguration {

    /**
     * Enables or disables the Vert.x cache.
     */
    @ConfigProperty(name = "caching", defaultValue = "true")
    public boolean fileResolverCachingEnabled;

    /**
     * Enables or disabled the Vert.x classpath resource resolver.
     */
    @ConfigProperty(name = "classpathResolving", defaultValue = "true")
    public boolean classpathResolvingEnabled;

    /**
     * The number of event loops. 2 x the number of core by default.
     */
    @ConfigProperty(name = "eventLoopsPoolSize", defaultValue = "-1")
    public int eventLoopsPoolSize;

//   TODO Wait until the long support is implemented.
//    /**
//     * The maximum amount of time the event loop can be blocked. In nano seconds.
//     */
//    @ConfigProperty(name = "maxEventLoopExecuteTime", defaultValue = "2000000000")
//    public long maxEventLoopExecuteTime;

    /**
     * The amount of time before a warning is displayed if the event loop is blocked. In milliseconds.
     */
    @ConfigProperty(name = "warningExceptionTime", defaultValue = "2000")
    public int warningExceptionTime;

    /**
     * The size of the worker thread pool.
     */
    @ConfigProperty(name = "workerPoolSize", defaultValue = "20")
    public int workerPoolSize;

//    TODO Wait until the long support is implemented.
//    /**
//     * The maximum amount of time the worker thread can be blocked. In nano seconds.
//     */
//    @ConfigProperty(name = "maxWorkerExecuteTime", defaultValue = "60000000000")
//    public long maxWorkerExecuteTime;

    /**
     * The size of the internal thread pool (used for the file system).
     */
    @ConfigProperty(name = "internalBlockingPoolSize", defaultValue = "20")
    public int internalBlockingPoolSize;

    /**
     * Enables the async DNS resolver.
     */
    @ConfigProperty(name = "useAsyncDNS", defaultValue = "false")
    public boolean useAsyncDNS;

    /**
     * The event bus configuration.
     */
    @ConfigProperty(name = "eventbus")
    public Optional<EventBusConfiguration> eventBusConfiguration;

    /**
     * The cluster configuration.
     */
    @ConfigProperty(name = "cluster")
    public Optional<ClusterConfiguration> clusterConfiguration;


}
