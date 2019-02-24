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

package io.quarkus.arc.processor;

/**
 * Allows a build-time extension to extend the original deployment.
 *
 * @author Martin Kouba
 */
public interface DeploymentEnhancer extends BuildExtension {

    /**
     *
     * @param deploymentContext
     */
    void enhance(DeploymentContext deploymentContext);

    interface DeploymentContext extends BuildContext {

        /**
         *
         * @param clazz
         * @throws IllegalArgumentException If the class cannot be added to the index
         */
        void addClass(Class<?> clazz);

        /**
         *
         * @param className The fully qualified class name
         * @throws IllegalArgumentException If the class cannot be added to the index
         */
        void addClass(String className);

    }

}
