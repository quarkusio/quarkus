package org.hibernate.protean.entityenhancement;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;

final class ProteanProxyFactory implements ProxyFactory {

	private final SessionFactoryImplementor sessionFactory;

	public ProteanProxyFactory(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void postInstantiate(
			String entityName,
			Class persistentClass,
			Set<Class> interfaces,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			CompositeType componentIdType) throws HibernateException {

	}

	@Override
	public HibernateProxy getProxy(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
		return null;
	}
}
