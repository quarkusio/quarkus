package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Env;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.Port;
import io.smallrye.config.WithDefault;

public interface DebugConfig {
    String PORT_NAME = "debug";
    String JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS";
    String AGENTLIB_FORMAT = "-agentlib:jdwp=transport=%s,server=y,suspend=%s,address=%s";

    /**
     * If true, the debug mode in pods will be enabled.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The transport to use.
     */
    @WithDefault("dt_socket")
    String transport();

    /**
     * If enabled, it means the JVM will wait for the debugger to attach before executing the main class. If false,
     * the JVM will immediately execute the main class, while listening for the debugger connection.
     */
    @WithDefault("n")
    String suspend();

    /**
     * It specifies the address at which the debug socket will listen.
     */
    @WithDefault("5005")
    Integer addressPort();

    default Env buildJavaToolOptionsEnv() {
        return new EnvBuilder()
                .withName(JAVA_TOOL_OPTIONS)
                .withValue(String.format(AGENTLIB_FORMAT, transport(), suspend(), addressPort()))
                .build();
    }

    default Port buildDebugPort() {
        return Port.newBuilder()
                .withName(PORT_NAME)
                .withContainerPort(addressPort())
                .build();
    }
}
