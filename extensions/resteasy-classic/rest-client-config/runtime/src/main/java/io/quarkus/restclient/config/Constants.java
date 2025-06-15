package io.quarkus.restclient.config;

public final class Constants {

    public static final String QUARKUS_CONFIG_PREFIX = "quarkus.rest-client.";
    public static final String MP_REST = "/mp-rest/";
    public static final String MP_REST_SCOPE_FORMAT = "%s" + MP_REST + "scope";
    public static final String QUARKUS_REST_SCOPE_FORMAT = QUARKUS_CONFIG_PREFIX + "%s.scope";
    public static final String GLOBAL_REST_SCOPE_FORMAT = QUARKUS_CONFIG_PREFIX + "scope";

    private Constants() {
    }

}
