package io.quarkus.qute.runtime.extensions;

import static io.quarkus.qute.TemplateExtension.ANY;
import static io.quarkus.qute.TemplateExtension.DEFAULT_PRIORITY;

import java.util.Optional;

import javax.enterprise.inject.Vetoed;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.qute.Results;
import io.quarkus.qute.TemplateExtension;

@Vetoed // Make sure no bean is created from this class
public class ConfigTemplateExtensions {

    static final String CONFIG = "config";

    // {config:foo} or {config:['bar.baz']}
    @TemplateExtension(namespace = CONFIG, matchName = ANY)
    static Object getConfigProperty(String propertyName) {
        return property(propertyName);
    }

    // {config:property(foo.getPropertyName())}
    @TemplateExtension(namespace = CONFIG, priority = DEFAULT_PRIORITY + 1)
    static Object property(String propertyName) {
        Optional<String> val = ConfigProvider.getConfig().getOptionalValue(propertyName, String.class);
        return val.isPresent() ? val.get() : Results.NotFound.from(propertyName);
    }

}
