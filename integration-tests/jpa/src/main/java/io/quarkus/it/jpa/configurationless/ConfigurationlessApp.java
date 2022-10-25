package io.quarkus.it.jpa.configurationless;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ApplicationPath("/jpa-test")
public class ConfigurationlessApp extends Application {
}
