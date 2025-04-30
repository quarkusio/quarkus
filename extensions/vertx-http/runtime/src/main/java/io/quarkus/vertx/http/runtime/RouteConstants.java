package io.quarkus.vertx.http.runtime;

/**
 * Route order value constants used in Quarkus, update {@code http-reference.adoc} when changing this class.
 */
@SuppressWarnings("JavadocDeclaration")
public final class RouteConstants {
    private RouteConstants() {
    }

    /**
     * Order value ({@value #ROUTE_ORDER_ACCESS_LOG_HANDLER}) for the access-log handler, if enabled in the configuration.
     */
    public static final int ROUTE_ORDER_ACCESS_LOG_HANDLER = Integer.MIN_VALUE;
    /**
     * Order value ({@value #ROUTE_ORDER_RECORD_START_TIME}) for the handler adding the start-time, if enabled in the
     * configuration.
     */
    public static final int ROUTE_ORDER_RECORD_START_TIME = Integer.MIN_VALUE;
    /**
     * Order value ({@value #ROUTE_ORDER_HOT_REPLACEMENT}) for the hot-replacement body handler.
     */
    public static final int ROUTE_ORDER_HOT_REPLACEMENT = Integer.MIN_VALUE;
    /**
     * Order value ({@value #ROUTE_ORDER_BODY_HANDLER_MANAGEMENT}) for the body handler for the management router.
     */
    public static final int ROUTE_ORDER_BODY_HANDLER_MANAGEMENT = Integer.MIN_VALUE;
    /**
     * Order value ({@value #ROUTE_ORDER_HEADERS}) for the handlers that add headers specified in the configuration.
     */
    public static final int ROUTE_ORDER_HEADERS = Integer.MIN_VALUE;
    /**
     * Order value ({@value #ROUTE_ORDER_CORS_MANAGEMENT}) for the CORS-Origin handler of the management router.
     */
    public static final int ROUTE_ORDER_CORS_MANAGEMENT = Integer.MIN_VALUE;
    /**
     * Order value ({@value #ROUTE_ORDER_BODY_HANDLER}) for the body handler.
     */
    public static final int ROUTE_ORDER_BODY_HANDLER = Integer.MIN_VALUE + 1;
    /**
     * Order value ({@value #ROUTE_ORDER_UPLOAD_LIMIT}) for the route that enforces the upload body size limit.
     */
    public static final int ROUTE_ORDER_UPLOAD_LIMIT = -2;
    /**
     * Order value ({@value #ROUTE_ORDER_COMPRESSION}) for the compression handler.
     */
    public static final int ROUTE_ORDER_COMPRESSION = 0;
    /**
     * Order value ({@value #ROUTE_ORDER_BEFORE_DEFAULT}) for route with priority over the default route (add an offset from
     * this value)
     */
    public static final int ROUTE_ORDER_BEFORE_DEFAULT = 1_000;
    /**
     * Default route order (i.e. Static Resources, Servlet): ({@value #ROUTE_ORDER_DEFAULT})
     */
    public static final int ROUTE_ORDER_DEFAULT = 10_000;
    /**
     * Order value ({@value #ROUTE_ORDER_AFTER_DEFAULT}) for route without priority over the default route (add an offset
     * from this value)
     */
    public static final int ROUTE_ORDER_AFTER_DEFAULT = 20_000;
}
