package io.quarkus.gradle.init;

import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.buildinit.specs.internal.BuildInitSpecRegistry;

/**
 * Supplies the {@code quarkus-app} build init spec so it can be picked up by
 * {@code gradle init -Dorg.gradle.buildinit.specs=<this-plugin-coordinates> --type quarkus-app}.
 */
public class QuarkusInitSpecsPlugin implements Plugin<Settings> {

    private final BuildInitSpecRegistry registry;

    @Inject
    public QuarkusInitSpecsPlugin(BuildInitSpecRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void apply(Settings settings) {
        registry.register(QuarkusAppInitGenerator.class, List.of(new QuarkusAppInitSpec()));
    }
}
