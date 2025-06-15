package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

public class CovariantReturnSubresourceLocatorsSubProxySubImpl implements CovariantReturnSubresourceLocatorsSubProxy {
    private final String path;

    public CovariantReturnSubresourceLocatorsSubProxySubImpl(final String path) {
        this.path = path;
    }

    @Override
    public String get() {
        return "Boo! - " + path;
    }
}
