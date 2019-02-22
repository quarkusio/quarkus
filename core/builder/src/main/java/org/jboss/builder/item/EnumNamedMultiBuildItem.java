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

package org.jboss.builder.item;

/**
 * A multi build item which has a name that is an enum constant.
 *
 * @param <E> the item type, which extends {@code Enum<?>} instead of {@code Enum<E>} to allow for items
 *        which can be named for any enum value
 */
@SuppressWarnings("unused")
public abstract class EnumNamedMultiBuildItem<E extends Enum<?>> extends NamedMultiBuildItem<E> {
    protected EnumNamedMultiBuildItem() {
        super();
    }
}
