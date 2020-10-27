package io.quarkus.rest.server.test.resource.basic.resource;

public class MultiInterfaceResLocatorSubresource implements MultiInterfaceResLocatorIntf1, MultiInterfaceResLocatorIntf2 {
    @Override
    public String resourceMethod1() {
        return "resourceMethod1";
    }

    @Override
    public String resourceMethod2() {
        return "resourceMethod2";
    }
}
