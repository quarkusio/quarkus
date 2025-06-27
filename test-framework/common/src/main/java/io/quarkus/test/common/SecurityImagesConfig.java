package io.quarkus.test.common;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration for security-related container images used in Quarkus tests.
 */
@ConfigMapping(prefix = "quarkus.container.image.security")
public interface SecurityImagesConfig {


    @WithName("registry")
    @WithDefault("docker.io")
    String registry();


    @WithName("keycloak.registry")
    @WithDefault("quay.io")
    String keycloakRegistry();


    @WithName("keycloak")
    @WithDefault("keycloak/keycloak:${keycloak.version}")
    String keycloakImage();


    @WithName("keycloak-legacy")
    @WithDefault("keycloak/keycloak:${keycloak.wildfly.version}-legacy")
    String keycloakLegacyImage();


    @WithName("ldap")
    @WithDefault("osixia/openldap:1.5.0")
    String ldapImage();


    default String getKeycloakFullImage() {
        return keycloakRegistry() + "/" + keycloakImage();
    }


    default String getKeycloakLegacyFullImage() {
        return keycloakRegistry() + "/" + keycloakLegacyImage();
    }


    default String getLdapFullImage() {
        return registry() + "/" + ldapImage();
    }
}