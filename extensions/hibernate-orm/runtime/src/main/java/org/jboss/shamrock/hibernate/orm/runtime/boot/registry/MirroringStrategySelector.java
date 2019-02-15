/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.hibernate.orm.runtime.boot.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

import org.hibernate.boot.registry.selector.spi.StrategyCreator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;

/**
 * FIXME: Apparently only used by extension points we don't support yet. The
 * idea is to intercept and record the answers from the original
 * StrategySelectorImpl and have this parrot the same answers.
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
