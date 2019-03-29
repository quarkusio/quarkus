package io.quarkus.extest.runtime.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "root", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class TestConfigRoot {
    /**
     * resource location of the DSAPublicKey encoded bytes
     */
    @ConfigProperty
    public String dsaKeyLocation;
}
