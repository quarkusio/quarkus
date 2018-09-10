package org.hibernate.protean.impl.serviceregistry;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.hibernate.boot.registry.selector.spi.StrategyCreator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;

/**
 * FIXME: Apparently only used by extension points we don't support yet.
 * The idea is to intercept and record the answers from the original StrategySelectorImpl
 * and have this parrot the same answers.
 */
public class MirroringStrategySelector implements StrategySelector {

	@Override
	public <T> void registerStrategyImplementor(Class<T> aClass, String s, Class<? extends T> aClass1) {

	}

	@Override
	public <T> void unRegisterStrategyImplementor(Class<T> aClass, Class<? extends T> aClass1) {

	}

	@Override
	public <T> Class<? extends T> selectStrategyImplementor(Class<T> aClass, String s) {
		return null;
	}

	@Override
	public <T> T resolveStrategy(Class<T> aClass, Object o) {
		return null;
	}

	@Override
	public <T> T resolveDefaultableStrategy(Class<T> aClass, Object o, T t) {
		return null;
	}

	@Override
	public <T> T resolveDefaultableStrategy(Class<T> aClass, Object o, Callable<T> callable) {
		return null;
	}

	@Override
	public <T> T resolveStrategy(Class<T> aClass, Object o, Callable<T> callable, StrategyCreator<T> strategyCreator) {
		return null;
	}

	@Override
	public <T> T resolveStrategy(Class<T> aClass, Object o, T t, StrategyCreator<T> strategyCreator) {
		return null;
	}

	@Override
	public <T> Collection<Class<? extends T>> getRegisteredStrategyImplementors(Class<T> aClass) {
		return Collections.EMPTY_SET;
	}

}
