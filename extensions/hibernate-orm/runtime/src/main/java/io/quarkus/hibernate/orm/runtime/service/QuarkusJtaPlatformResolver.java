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

package io.quarkus.hibernate.orm.runtime.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformResolver;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class QuarkusJtaPlatformResolver implements StandardServiceInitiator<JtaPlatformResolver> {

    private final JtaPlatform jtaPlatform;

    public QuarkusJtaPlatformResolver(JtaPlatform jtaPlatform) {
        this.jtaPlatform = jtaPlatform;
    }

    @Override
    public JtaPlatformResolver initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new JtaPlatformResolver() {
            @Override
            public JtaPlatform resolveJtaPlatform(Map configurationValues, ServiceRegistryImplementor registry) {
                return jtaPlatform;
            }
        };
    }

    @Override
    public Class<JtaPlatformResolver> getServiceInitiated() {
        return JtaPlatformResolver.class;
    }
}
