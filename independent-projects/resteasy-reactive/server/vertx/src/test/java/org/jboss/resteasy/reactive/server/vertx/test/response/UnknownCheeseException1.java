package org.jboss.resteasy.reactive.server.vertx.test.response;

@SuppressWarnings("serial")
public class UnknownCheeseException1 extends RuntimeException {

    public String name;

    public UnknownCheeseException1(String name) {
        this.name = name;
    }
}
