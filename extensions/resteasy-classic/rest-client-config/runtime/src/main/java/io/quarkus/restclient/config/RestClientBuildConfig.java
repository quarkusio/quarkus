package io.quarkus.restclient.config;

import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RestClientBuildConfig {

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
    @ConfigItem
    public Optional<String> scope;
}
