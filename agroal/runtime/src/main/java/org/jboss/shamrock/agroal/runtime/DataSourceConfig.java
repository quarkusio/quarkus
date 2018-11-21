package org.jboss.shamrock.agroal.runtime;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfiguredType;

@ConfiguredType
public class DataSourceConfig {

    /**
     * The datasource driver class name
     */
    @ConfigProperty(name = "driver")
    public String driver;

    /**
     * The datasource URL
     */
    @ConfigProperty(name = "url")
    public String url;

    /**
     * The datasource username
     */
    @ConfigProperty(name = "username")
    public Optional<String> user;

    /**
     * The datasource password
     */
    @ConfigProperty(name = "password")
    public Optional<String> password;

    /**
     * The datasource pool minimum size
     */
    @ConfigProperty(name = "minSize", defaultValue = "5")
    public int minSize;

    /**
     * The datasource pool maximum size
     */
    @ConfigProperty(name = "maxSize", defaultValue = "20")
    public int maxSize;

}
