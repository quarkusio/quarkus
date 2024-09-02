package io.quarkus.restclient.config;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.rest-client")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface RestClientsBuildTimeConfig {
    /**
     * Configurations of REST client instances.
     */
    @WithParentName
    @WithDefaults
    Map<String, RestClientBuildConfig> clients();

    interface RestClientBuildConfig {

        /**
         * The CDI scope to use for injection. This property can contain either a fully qualified class name of a CDI scope
         * annotation (such as "jakarta.enterprise.context.ApplicationScoped") or its simple name (such as
         * "ApplicationScoped").
         * By default, this is not set which means the interface is not registered as a bean unless it is annotated with
         * {@link RegisterRestClient}.
         * If an interface is not annotated with {@link RegisterRestClient} and this property is set, then Quarkus will make the
         * interface
         * a bean of the configured scope.
         */
        Optional<String> scope();

        /**
         * If set to true, then Quarkus will ensure that all calls from the REST client go through a local proxy
         * server (that is managed by Quarkus).
         * This can be very useful for capturing network traffic to a service that uses HTTPS.
         * <p>
         * This property is not applicable to the RESTEasy Client, only the Quarkus REST client (formerly RESTEasy Reactive
         * client).
         * <p>
         * This property only applicable to dev and test mode.
         */
        @WithDefault("false")
        boolean enableLocalProxy();

        /**
         * This setting is used to select which proxy provider to use if there are multiple ones.
         * It only applies if {@code enable-local-proxy} is true.
         * <p>
         * The algorithm for picking between multiple provider is the following:
         * <ul>
         * <li>If only the default is around, use it (its name is {@code default})</li>
         * <li>If there is only one besides the default, use it</li>
         * <li>If there are multiple ones, fail</li>
         * </ul>
         */
        Optional<String> localProxyProvider();
    }
}
