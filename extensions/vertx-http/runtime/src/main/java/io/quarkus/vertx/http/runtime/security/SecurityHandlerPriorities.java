package io.quarkus.vertx.http.runtime.security;

public class SecurityHandlerPriorities {

    public static final int CORS = 300;
    public static final int AUTHENTICATION = 200;
    public static final int FORM_AUTHENTICATION = 150;
    public static final int AUTHORIZATION = 100;
    public static final int AUTH_FAILURE_HANDLER = Integer.MIN_VALUE + 1;
}
