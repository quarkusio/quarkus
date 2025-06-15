package io.quarkus.it.mockbean.hello;

import io.quarkus.it.mockbean.HelloService;

class HelloServiceImpl implements HelloService {

    private final RecordB recB;

    HelloServiceImpl(RecordB recB) {
        this.recB = recB;
    }

    @Override
    public String hello() {
        return recB.dataHello();
    }

}
