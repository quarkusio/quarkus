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

package io.quarkus.hibernate.orm.runtime.graal;

import java.util.Map;

import javax.management.ObjectName;

import org.hibernate.jmx.spi.JmxService;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Manageable;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.hibernate.jmx.internal.JmxServiceImpl")
@Substitute
public final class Substitute_JmxServiceImpl implements JmxService {

    @Substitute
    @SuppressWarnings("rawtypes")
    public Substitute_JmxServiceImpl(Map configValues) {
        // ignored but needs to exist
    }

    @Override
    public void registerService(Manageable service, Class<? extends Service> serviceRole) {
        // no-op
    }

    @Override
    public void registerMBean(ObjectName objectName, Object mBean) {
        // no-op
    }
}
