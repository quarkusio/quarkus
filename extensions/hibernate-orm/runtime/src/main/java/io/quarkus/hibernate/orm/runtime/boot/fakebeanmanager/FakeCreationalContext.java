package io.quarkus.hibernate.orm.runtime.boot.fakebeanmanager;

import javax.enterprise.context.spi.CreationalContext;

class FakeCreationalContext<T> implements CreationalContext<T> {
    @Override
    public void push(T incompleteInstance) {

    }

    @Override
    public void release() {

    }
}
