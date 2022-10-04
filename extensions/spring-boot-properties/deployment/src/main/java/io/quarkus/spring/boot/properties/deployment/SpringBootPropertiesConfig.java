package io.quarkus.spring.boot.properties.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class SpringBootPropertiesConfig {

    /**
     * The naming strategy used for {@code org.springframework.boot.context.properties.ConfigurationProperties}.
     */
    @ConfigItem(defaultValue = "kebab-case")
    public ConfigMapping.NamingStrategy configurationPropertiesNamingStrategy;

}
