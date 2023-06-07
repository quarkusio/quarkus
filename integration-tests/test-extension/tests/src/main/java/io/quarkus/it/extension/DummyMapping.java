package io.quarkus.it.extension;

import io.quarkus.arc.Unremovable;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "dummy")
@Unremovable
public interface DummyMapping {

    @WithDefault("foo")
    String name();

    @WithDefault("50")
    int age();
}
