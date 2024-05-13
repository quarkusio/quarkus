package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import org.jboss.logging.Logger;

public class ParameterSubResSubImpl<T extends Number> implements ParameterSubResSub, ParameterSubResInternalInterface<T> {
    private static Logger LOG = Logger.getLogger(ParameterSubResSubImpl.class);

    private final String path;

    public ParameterSubResSubImpl(final String path) {
        this.path = path;
    }

    @Override
    public String get() {
        return "Boo! - " + path;
    }

    @Override
    public void foo(T value) {
        LOG.debug("foo: " + value);
    }

}
