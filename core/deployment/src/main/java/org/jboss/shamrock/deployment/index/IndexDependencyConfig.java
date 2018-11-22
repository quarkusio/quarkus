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

package org.jboss.shamrock.deployment.index;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfigGroup;

@ConfigGroup
public class IndexDependencyConfig {

    /**
     * The maven groupId of the artifact to index
     */
    @ConfigProperty(name = "groupId")
    String groupId;

    /**
     * The maven artifactId of the artifact to index
     */
    @ConfigProperty(name = "artifactId")
    String artifactId;

    /**
     * The maven classifier of the artifact to index
     */
    @ConfigProperty(name = "classifier", defaultValue = "")
    String classifier;

}
