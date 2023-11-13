package org.acme.example.extension.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class ExampleConfig {

    /**
     * name
     */
    @ConfigItem(defaultValue = "none")
    String name;
}