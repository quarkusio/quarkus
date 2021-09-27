package io.quarkus.hibernate.orm.runtime.customized;

import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.ProxyFactory;

import io.quarkus.hibernate.orm.runtime.proxies.ProxyDefinitions;

/**
 * This ProxyFactoryFactory is responsible to loading proxies which have been
 * defined in advance.
 * N.B. during the Quarkus application build, the service registry will use a different implementation:
 * 
 * @see io.quarkus.hibernate.orm.runtime.customized.BootstrapOnlyProxyFactoryFactoryInitiator
 */
public class QuarkusRuntimeProxyFactoryFactory implements ProxyFactoryFactory {

    private final ProxyDefinitions proxyClassDefinitions;

    public QuarkusRuntimeProxyFactoryFactory(ProxyDefinitions proxyClassDefinitions) {
        this.proxyClassDefinitions = proxyClassDefinitions;
    }

    @Override
    public ProxyFactory buildProxyFactory(SessionFactoryImplementor sessionFactory) {
        return new QuarkusProxyFactory(proxyClassDefinitions);
    }

    @Deprecated
    @Override
    public BasicProxyFactory buildBasicProxyFactory(Class superClass, Class[] interfaces) {
        return null;
    }

    @Override
    public BasicProxyFactory buildBasicProxyFactory(Class aClass) {
        return null;
    }
}
