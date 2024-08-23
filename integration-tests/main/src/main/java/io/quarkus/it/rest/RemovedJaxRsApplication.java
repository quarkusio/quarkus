package io.quarkus.it.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * This class is actually never used, because we configure Quarkus to remove it from Class Loading and build time indexing
 */
@ApplicationPath("unused")
public class RemovedJaxRsApplication extends Application {
}
