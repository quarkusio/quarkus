package org.jboss.resteasy.reactive.server.vertx.test.simple;

import jakarta.inject.Inject;

public class InterfaceResourceImpl implements InterfaceResource {

    @Inject
    HelloService helloService;

    @Override
    public String hello() {
        return helloService.sayHello();
    }
}
