package org.hibernate.protean.entityenhancement;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;

public class UpfrontDefinedBytecodeProvider implements BytecodeProvider {

	@Override
	public ProxyFactoryFactory getProxyFactoryFactory() {
		return new ProteanProxyFactoryFactory();
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer(Class clazz, String[] getterNames, String[] setterNames, Class[] types) {
		return null;
	}

	@Override
	public Enhancer getEnhancer(EnhancementContext enhancementContext) {
		return null;
	}
}
