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

package org.jboss.shamrock.jaxrs.runtime.graal;

import java.lang.reflect.Constructor;

import org.jboss.logging.Logger;
import org.jboss.resteasy.core.InjectorFactoryImpl;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.ResourceConstructor;
import org.jboss.shamrock.runtime.cdi.BeanContainer;

/**
 * Created by bob on 7/31/18.
 */
public class ShamrockInjectorFactory extends InjectorFactoryImpl {

    private static final Logger log = Logger.getLogger("org.jboss.shamrock.jaxrs.runtime");
    public static volatile BeanContainer CONTAINER = null;

    @Override
    public ConstructorInjector createConstructor(Constructor constructor, ResteasyProviderFactory providerFactory) {
        log.debugf("Create constructor: %s", constructor);
        return super.createConstructor(constructor, providerFactory);
    }

    @Override
    public ConstructorInjector createConstructor(ResourceConstructor constructor, ResteasyProviderFactory providerFactory) {
        log.debugf("Create resource constructor: %s", constructor.getConstructor());
        return new ShamrockConstructorInjector(constructor.getConstructor(), super.createConstructor(constructor, providerFactory));
    }
}
