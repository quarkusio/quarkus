/*
 * Copyright 2019 Red Hat, Inc.
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
package io.quarkus.reactivemessaging.smallrye.deployment;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.BeanInfo;

public final class MediatorBuildItem extends MultiBuildItem {

    private final BeanInfo bean;

    private final MethodInfo method;

    public MediatorBuildItem(BeanInfo bean, MethodInfo method) {
        this.bean = bean;
        this.method = method;
    }

    public BeanInfo getBean() {
        return bean;
    }

    public MethodInfo getMethod() {
        return method;
    }

}
