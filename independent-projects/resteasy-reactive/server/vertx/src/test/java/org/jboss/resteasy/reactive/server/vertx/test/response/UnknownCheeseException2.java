package org.jboss.resteasy.reactive.server.vertx.test.response;

@SuppressWarnings("serial")
public class UnknownCheeseException2 extends RuntimeException {

    public String name;

    public UnknownCheeseException2(String name) {
        this.name = name;
    }
}
