package io.quarkus.it.hibernate.validator.injection;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "io.quarkus.it.hibernate.validator.injection")
public interface MyRuntimeConfiguration {

    String pattern();
}
