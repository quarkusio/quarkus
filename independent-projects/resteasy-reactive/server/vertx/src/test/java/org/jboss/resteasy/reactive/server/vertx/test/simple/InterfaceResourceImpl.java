package org.jboss.resteasy.reactive.server.vertx.test.simple;

public class InterfaceResourceImpl implements InterfaceResource {

    @Override
    public String hello() {
        return "Hello";
    }
}
