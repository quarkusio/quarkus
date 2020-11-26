package io.quarkus.rest.server.test.resource.basic.resource;

public class CovariantReturnSubresourceLocatorsSubProxySubImpl implements CovariantReturnSubresourceLocatorsSubProxy {
    private final String path;

    public CovariantReturnSubresourceLocatorsSubProxySubImpl(final String path) {
        this.path = path;
    }

    public String get() {
        return "Boo! - " + path;
    }
}
