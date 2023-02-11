package io.quarkus.hibernate.orm.runtime.service.bytecodeprovider;

import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;

final class DisallowedProxyFactory implements ProxyFactory {

    static final DisallowedProxyFactory INSTANCE = new DisallowedProxyFactory();

    @Override
    public void postInstantiate(
            String entityName,
            Class<?> persistentClass,
            Set<Class<?>> interfaces,
            Method getIdentifierMethod,
            Method setIdentifierMethod,
            CompositeType componentIdType) {
    }

    @Override
    public HibernateProxy getProxy(Object id, SharedSessionContractImplementor session) throws HibernateException {
        throw new HibernateException(
                "Generation of HibernateProxy instances at runtime is not allowed when the configured BytecodeProvider is 'none'; your model requires a more advanced BytecodeProvider to be enabled.");
    }

}
