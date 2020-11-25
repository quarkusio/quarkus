package io.quarkus.rest.server.test.resource.basic.resource;

public class ScanResource implements ScanProxy {
    public ScanSubresource doit() {
        return new ScanSubresource();
    }

    public String get() {
        return "hello world";
    }
}
