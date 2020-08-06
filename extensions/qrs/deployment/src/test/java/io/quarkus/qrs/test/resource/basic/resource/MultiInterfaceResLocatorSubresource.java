package io.quarkus.qrs.test.resource.basic.resource;

import javax.ws.rs.Path;

@Path("")
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
