package io.quarkus.it.mockbean;

import io.quarkus.arc.Unremovable;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "dummy")
@Unremovable
public interface DummyMapping {
}
