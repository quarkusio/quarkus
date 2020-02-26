package io.quarkus.vertx.http.runtime.devmode;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;

public class ConfigHolder {

    @Inject
    Config config;
}
