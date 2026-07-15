package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;

public interface GatewayConfig {

    /**
     * If true, generate Gateway API resources to expose the application.
     */
    @WithDefault("false")
    boolean expose();

    /**
     * If true, also generate a Gateway resource. Usually the platform already provides a shared Gateway;
     * in that case leave this false and configure {@code parent-refs} instead.
     */
    @WithDefault("false")
    boolean generateGateway();

    /**
     * The GatewayClass name to use when {@code generate-gateway} is true.
     */
    Optional<String> gatewayClassName();

    /**
     * The host under which the application is going to be exposed (HTTPRoute hostname).
     */
    Optional<String> host();

    /**
     * Additional hostnames for the HTTPRoute. Combined with {@code host} when both are set.
     */
    Optional<List<String>> hosts();

    /**
     * The default path match value. Default is {@code /}.
     * Can also be overridden via {@code quarkus.kubernetes.ports.<target-port>.path}.
     */
    @WithDefault("/")
    String path();

    /**
     * The path match type. One of {@code Exact}, {@code PathPrefix}, or {@code RegularExpression}.
     */
    @WithDefault("PathPrefix")
    String pathType();

    /**
     * The default target named port. If not provided matching ports exist, it falls back to {@code http}.
     */
    @WithDefault("http")
    String targetPort();

    /**
     * Custom annotations to add to the generated HTTPRoute (and Gateway when generated).
     */
    @ConfigDocMapKey("annotation-name")
    Map<String, String> annotations();

    /**
     * Parent Gateway references the HTTPRoute attaches to.
     * Required when {@code expose=true} and {@code generate-gateway=false}.
     */
    Map<String, ParentRefConfig> parentRefs();

    /**
     * Additional HTTPRoute rules (in addition to the default rule from path/target-port).
     */
    Map<String, GatewayRuleConfig> rules();

    /**
     * Listeners when {@code generate-gateway=true}. If empty, a default HTTP listener on port 80 named
     * {@code http} is used.
     */
    Map<String, ListenerConfig> listeners();

    interface ParentRefConfig {

        /**
         * Name of the parent Gateway.
         */
        String name();

        /**
         * Namespace of the parent Gateway. Defaults to the HTTPRoute namespace when omitted.
         */
        Optional<String> namespace();

        /**
         * Section name (listener name) on the parent Gateway.
         */
        Optional<String> sectionName();

        /**
         * Group of the parent resource. Defaults to {@code gateway.networking.k8s.io}.
         */
        Optional<String> group();

        /**
         * Kind of the parent resource. Defaults to {@code Gateway}.
         */
        Optional<String> kind();
    }

    interface GatewayRuleConfig {

        /**
         * The path under which the rule is going to be used. Default is {@code /}.
         */
        @WithDefault("/")
        String path();

        /**
         * The path match type. Default is {@code PathPrefix}.
         */
        @WithDefault("PathPrefix")
        String pathType();

        /**
         * The service name to be used by this rule. Default is the generated service name of the application.
         */
        Optional<String> serviceName();

        /**
         * The named service port to resolve to a number for this rule.
         * Used when {@code service-port-number} is not set.
         */
        Optional<String> servicePortName();

        /**
         * The service port number to be used by this rule.
         * Required by Gateway API {@code backendRefs} when {@code service-name} points to a different Service.
         * Preferred when set; otherwise the named port is resolved from the current application only.
         */
        Optional<Integer> servicePortNumber();
    }

    interface ListenerConfig {

        /**
         * Listener name (used as sectionName on HTTPRoute parentRefs).
         */
        String name();

        /**
         * Protocol for the listener: {@code HTTP}, {@code HTTPS}, {@code TLS}, {@code TCP}, or {@code UDP}.
         * Note: HTTPS/TLS certificateRefs are not generated yet; prefer {@code HTTP} unless TLS is configured
         * out-of-band on the Gateway.
         */
        @WithDefault("HTTP")
        String protocol();

        /**
         * Port the listener binds to.
         */
        @WithDefault("80")
        int port();

        /**
         * Optional hostname for the listener.
         */
        Optional<String> hostname();
    }
}
