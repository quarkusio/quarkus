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

package io.quarkus.arc.runtime;

/**
 * An interface that can be used to configure beans immediately after the {@link BeanContainer} has been
 * created. The container is passed to the interface and beans can be obtained and be modified.
 *
 * This provides a convenient way to pass configuration from the deployment processors into runtime beans.
 */
public interface BeanContainerListener {

    void created(BeanContainer container);

}
