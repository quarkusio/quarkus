package io.quarkus.arc.test.configproperties;

import io.quarkus.arc.config.ConfigProperties;

@ConfigProperties
public interface GreetingExtraProperties {

    String getMessage();
}
