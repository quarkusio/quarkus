package io.quarkus.resteasy.reactive.server.test.simple;

import jakarta.inject.Inject;

public class InterfaceResourceImpl implements InterfaceResource {

    @Inject
    HelloService helloService;

    @Override
    public String hello() {
        return helloService.sayHello();
    }
}
