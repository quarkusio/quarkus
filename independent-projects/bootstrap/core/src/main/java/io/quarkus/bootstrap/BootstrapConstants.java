/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.bootstrap;

/**
 *
 * @author Alexey Loubyansky
 */
public interface BootstrapConstants {

    String DEPLOYMENT_DEPENDENCY_GRAPH = "quarkus-deployment-dependency.graph";

    String DESCRIPTOR_FILE_NAME = "quarkus-extension.properties";

    String DESCRIPTOR_PATH = DESCRIPTOR_FILE_NAME;

    String META_INF = "META-INF";

    String PROP_DEPLOYMENT_ARTIFACT = "deployment-artifact";
}
