package io.quarkus.spring.boot.properties.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.spring-boot-properties")
public interface SpringBootPropertiesConfig {

    /**
     * The naming strategy used for {@code org.springframework.boot.context.properties.ConfigurationProperties}.
     */
    @WithDefault("kebab-case")
    ConfigMapping.NamingStrategy configurationPropertiesNamingStrategy();

}
