package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

public class ScanResource implements ScanProxy {
    @Override
    public ScanSubresource doit() {
        return new ScanSubresource();
    }

    @Override
    public String get() {
        return "hello world";
    }
}
