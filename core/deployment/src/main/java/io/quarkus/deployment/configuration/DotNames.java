package io.quarkus.deployment.configuration;

import org.jboss.jandex.DotName;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;

public class DotNames {
    public static final DotName CONFIG_MAPPING = DotName.createSimple(ConfigMapping.class);
    public static final DotName STATIC_INIT_SAFE = DotName.createSimple(StaticInitSafe.class);
}
