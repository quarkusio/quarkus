package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class IngressConfig {

    /**
     * If true, the service will be exposed
     */
    @ConfigItem
    boolean expose;

    /**
     * The host under which the application is going to be exposed
     */
    @ConfigItem
    Optional<String> host;

    /**
     * The default target named port. If not provided, it will be deducted from the Service resource ports.
     * Options are: "http" and "https".
     */
    @ConfigItem(defaultValue = "http")
    String targetPort;

    /**
     * The class of the Ingress. If the ingressClassName is omitted, a default Ingress class is used.
     */
    @ConfigItem
    Optional<String> ingressClassName;

    /**
     * Custom annotations to add to exposition (route or ingress) resources
     */
    @ConfigItem
    @ConfigDocMapKey("annotation-name")
    Map<String, String> annotations;

    /**
     * Allow to configure the TLS Ingress configuration hosts by secret name.
     */
    @ConfigItem
    Map<String, IngressTlsConfig> tls;

    /**
     * Custom rules for the current ingress resource.
     */
    @ConfigItem
    Map<String, IngressRuleConfig> rules;

}
