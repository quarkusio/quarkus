package org.hibernate.protean.entityenhancement;

import org.hibernate.bytecode.spi.BasicProxyFactory;

public class ProteanBasicProxyFactory implements BasicProxyFactory {

	private final Class superClass;
	private final Class[] interfaces;

	public ProteanBasicProxyFactory(Class superClass, Class[] interfaces) {
		this.superClass = superClass;
		this.interfaces = interfaces;
	}

	@Override
	public Object getProxy() {
		return null;
	}

}
