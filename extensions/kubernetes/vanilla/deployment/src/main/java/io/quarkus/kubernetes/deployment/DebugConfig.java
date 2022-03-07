package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Env;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.Port;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DebugConfig {

    private static final String PORT_NAME = "debug";
    private static final String JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS";
    private static final String AGENTLIB_FORMAT = "-agentlib:jdwp=transport=%s,server=y,suspend=%s,address=%s";

    /**
     * If true, the debug mode in pods will be enabled.
     */
    @ConfigItem(defaultValue = "false")
    boolean enabled;

    /**
     * The transport to use.
     */
    @ConfigItem(defaultValue = "dt_socket")
    String transport;

    /**
     * If enabled, it means the JVM will wait for the debugger to attach before executing the main class.
     * If false, the JVM will immediately execute the main class, while listening for
     * the debugger connection.
     */
    @ConfigItem(defaultValue = "n")
    String suspend;

    /**
     * It specifies the address at which the debug socket will listen.
     */
    @ConfigItem(defaultValue = "5005")
    Integer addressPort;

    protected Env buildJavaToolOptionsEnv() {
        return new EnvBuilder()
                .withName(JAVA_TOOL_OPTIONS)
                .withValue(String.format(AGENTLIB_FORMAT, transport, suspend, addressPort))
                .build();
    }

    protected Port buildDebugPort() {
        return Port.newBuilder()
                .withName(PORT_NAME)
                .withContainerPort(addressPort)
                .build();
    }
}
