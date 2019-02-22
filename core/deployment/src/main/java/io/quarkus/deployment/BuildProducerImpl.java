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

package io.quarkus.deployment;

import org.jboss.builder.BuildContext;
import org.jboss.builder.item.BuildItem;

import io.quarkus.deployment.annotations.BuildProducer;

/**
 * Producer class used by the source generated from the annotation processor
 * 
 * @param <T>
 */
@SuppressWarnings("unused")
public class BuildProducerImpl<T extends BuildItem> implements BuildProducer<T> {

    private final Class<T> type;
    private final BuildContext buildContext;

    public BuildProducerImpl(Class<T> type, BuildContext buildContext) {
        this.type = type;
        this.buildContext = buildContext;
    }

    @Override
    public void produce(T item) {
        buildContext.produce(type, item);
    }
}
