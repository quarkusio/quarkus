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
package org.jboss.shamrock.restclient.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

/**
 * Created by hbraun on 22.01.18.
 */
class ConfigurationWrapper implements Configuration {

    public ConfigurationWrapper(Configuration delegate) {
        this.delegate = delegate;
    }

    @Override
    public RuntimeType getRuntimeType() {
        return delegate.getRuntimeType();
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    @Override
    public Object getProperty(String name) {
        return delegate.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return delegate.getPropertyNames();
    }

    @Override
    public boolean isEnabled(Feature feature) {
        return delegate.isEnabled(feature);
    }

    @Override
    public boolean isEnabled(Class<? extends Feature> featureClass) {
        return delegate.isEnabled(featureClass);
    }

    @Override
    public boolean isRegistered(Object component) {
        return delegate.isRegistered(component);
    }

    @Override
    public boolean isRegistered(Class<?> componentClass) {
        return delegate.isRegistered(componentClass);
    }

    @Override
    public Map<Class<?>, Integer> getContracts(Class<?> componentClass) {
        Map<Class<?>, Integer> contracts = new HashMap<>();
        contracts.putAll(getLocalContracts(componentClass));
        contracts.putAll(delegate.getContracts(componentClass));
        return contracts;
    }

    private Map<Class<?>, ? extends Integer> getLocalContracts(Class<?> componentClass) {
        if (localClassContracts.containsKey(componentClass)) {
            return localClassContracts.get(componentClass);
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        return delegate.getClasses();
    }

    @Override
    public Set<Object> getInstances() {
        return delegate.getInstances();
    }

    void registerLocalContract(Class<?> provider, Map<Class<?>, Integer> contracts) {
        localClassContracts.put(provider, contracts);
    }

    protected Map<Class<?>, Map<Class<?>, Integer>> localClassContracts = new HashMap<>();

    private final Configuration delegate;
}
