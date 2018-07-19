package org.hibernate.protean.entityenhancement;

import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.ProxyFactory;

public class ProteanProxyFactoryFactory implements ProxyFactoryFactory {

	@Override
	public ProxyFactory buildProxyFactory(SessionFactoryImplementor sessionFactory) {
		return new ProteanProxyFactory( sessionFactory );
	}

	@Override
	public BasicProxyFactory buildBasicProxyFactory(Class superClass, Class[] interfaces) {
		return new ProteanBasicProxyFactory( superClass, interfaces );
	}
}
