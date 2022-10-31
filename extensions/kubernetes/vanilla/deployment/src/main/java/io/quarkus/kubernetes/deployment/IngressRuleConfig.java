package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class IngressRuleConfig {

    /**
     * The host under which the rule is going to be used.
     */
    @ConfigItem
    String host;

    /**
     * The path under which the rule is going to be used. Default is "/".
     */
    @ConfigItem(defaultValue = "/")
    String path;

    /**
     * The path type strategy to use by the Ingress rule. Default is "Prefix".
     */
    @ConfigItem(defaultValue = "Prefix")
    String pathType;

    /**
     * The service name to be used by this Ingress rule. Default is the generated service name of the application.
     */
    @ConfigItem
    Optional<String> serviceName;

    /**
     * The service port name to be used by this Ingress rule. Default is the port name of the generated service of
     * the application.
     */
    @ConfigItem
    Optional<String> servicePortName;

    /**
     * The service port number to be used by this Ingress rule. This is only used when the servicePortName is not set.
     */
    @ConfigItem
    Optional<Integer> servicePortNumber;

}
