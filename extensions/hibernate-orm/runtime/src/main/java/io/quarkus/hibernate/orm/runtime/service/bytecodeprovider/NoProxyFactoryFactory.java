package io.quarkus.hibernate.orm.runtime.service.bytecodeprovider;

import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.ProxyFactory;

final class NoProxyFactoryFactory implements ProxyFactoryFactory {

    @Override
    public ProxyFactory buildProxyFactory(SessionFactoryImplementor sessionFactory) {
        return DisallowedProxyFactory.INSTANCE;
    }

    @Override
    public BasicProxyFactory buildBasicProxyFactory(Class superClassOrInterface) {
        return new NoneBasicProxyFactory(superClassOrInterface);
    }
}
