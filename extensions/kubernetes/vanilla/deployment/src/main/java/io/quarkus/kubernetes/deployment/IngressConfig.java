package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;

public interface IngressConfig {
    /**
     * If true, the service will be exposed
     */
    @WithDefault("false")
    boolean expose();

    /**
     * The host under which the application is going to be exposed
     */
    Optional<String> host();

    /**
     * The default target named port. If not provided, it will be deducted from the Service resource ports.
     * Options are: "http" and "https".
     */
    @WithDefault("http")
    String targetPort();

    /**
     * The class of the Ingress. If the ingressClassName is omitted, a default Ingress class is used.
     */
    Optional<String> ingressClassName();

    /**
     * Custom annotations to add to exposition (route or ingress) resources
     */
    @ConfigDocMapKey("annotation-name")
    Map<String, String> annotations();

    /**
     * Allow to configure the TLS Ingress configuration hosts by secret name.
     */
    Map<String, IngressTlsConfig> tls();

    /**
     * Custom rules for the current ingress resource.
     */
    Map<String, IngressRuleConfig> rules();

    interface IngressTlsConfig {

        /**
         * If true, it will use the TLS configuration in the generated Ingress resource.
         */
        boolean enabled();

        /**
         * The list of hosts to be included in the TLS certificate. By default, it will use the application host.
         */
        Optional<List<String>> hosts();
    }

    interface IngressRuleConfig {

        /**
         * The host under which the rule is going to be used.
         */
        String host();

        /**
         * The path under which the rule is going to be used. Default is "/".
         */
        @WithDefault("/")
        String path();

        /**
         * The path type strategy to use by the Ingress rule. Default is "Prefix".
         */
        @WithDefault("Prefix")
        String pathType();

        /**
         * The service name to be used by this Ingress rule. Default is the generated service name of the application.
         */
        Optional<String> serviceName();

        /**
         * The service port name to be used by this Ingress rule. Default is the port name of the generated service of
         * the application.
         */
        Optional<String> servicePortName();

        /**
         * The service port number to be used by this Ingress rule. This is only used when the servicePortName is not set.
         */
        Optional<Integer> servicePortNumber();
    }
}
