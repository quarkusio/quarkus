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

package org.jboss.shamrock.annotations;

import org.jboss.builder.item.BuildItem;

/**
 * An interface that can be injected to produce {@link BuildItem} instances
 *
 * This can be injected into either a field or method parameter. To produce
 * a {@link BuildItem} simply call the {@link #produce(BuildItem)} method
 * with the instance.
 *
 *
 * @param <T> The type of build item to produce
 */
public interface BuildProducer<T extends BuildItem> {

    void produce(T item);

}
