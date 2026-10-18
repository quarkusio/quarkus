package io.quarkus.resteasy.test;

public class InterfaceResourceImpl implements InterfaceResource {

    private Service service;

    public InterfaceResourceImpl(Service service) {
        this.service = service;
    }

    @Override
    public String hello() {
        return "hello from impl";
    }
}
