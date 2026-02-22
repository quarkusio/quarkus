module io.quarkus.vertx.utils {
    requires io.netty.buffer;
    requires io.netty.codec.http;
    requires io.netty.common;
    requires io.vertx.core;
    requires io.vertx.web;
    requires org.jboss.logging;

    exports io.quarkus.vertx.utils;
}
