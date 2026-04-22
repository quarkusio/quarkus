package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * This {@link ProxyFactoryFactory} is not expected to be actually used:
 * it's just a placeholder for static init, during which Hibernate ORM Metadata is generated,
 * which (together with build-time proxy generation)
 * enables creating the actual {@link QuarkusProxyFactory} for use at runtime.
 *
 * @see QuarkusRuntimeProxyFactoryFactory
 */
public class QuarkusStaticInitProxyFactoryFactory implements ProxyFactoryFactory {

    public QuarkusStaticInitProxyFactoryFactory() {
    }

    @Override
    public ProxyFactory buildProxyFactory(SessionFactoryImplementor sessionFactory) {
        throw shouldNotBeCalled();
    }

    @Override
    public BasicProxyFactory buildBasicProxyFactory(Class aClass) {
        throw shouldNotBeCalled();
    }

    private RuntimeException shouldNotBeCalled() {
        return new AssertionFailure(
                "ProxyFactoryFactory called during static initialization. There is a bug in Quarkus, please report it.");
    }

    public static final class Initiator implements StandardServiceInitiator<ProxyFactoryFactory> {

        public static final StandardServiceInitiator<ProxyFactoryFactory> INSTANCE = new Initiator();

        @Override
        public ProxyFactoryFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
            return new QuarkusStaticInitProxyFactoryFactory();
        }

        @Override
        public Class<ProxyFactoryFactory> getServiceInitiated() {
            return ProxyFactoryFactory.class;
        }
    }
}
