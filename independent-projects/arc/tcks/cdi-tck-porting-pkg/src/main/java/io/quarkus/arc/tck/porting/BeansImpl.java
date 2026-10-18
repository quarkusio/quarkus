package io.quarkus.arc.tck.porting;

import org.jboss.cdi.tck.spi.Beans;

import io.quarkus.arc.ClientProxy;

public class BeansImpl implements Beans {
    @Override
    public boolean isProxy(Object o) {
        return o instanceof ClientProxy;
    }

    @Override
    public byte[] passivate(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object activate(byte[] bytes) {
        throw new UnsupportedOperationException();
    }
}
