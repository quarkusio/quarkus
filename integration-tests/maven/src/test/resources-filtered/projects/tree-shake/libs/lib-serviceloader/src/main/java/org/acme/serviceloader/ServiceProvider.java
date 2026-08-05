package org.acme.serviceloader;

public class ServiceProvider implements ServiceInterface {
    @Override
    public String serve() {
        return "ServiceProvider";
    }
}
