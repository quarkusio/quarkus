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
package io.quarkus.gradle.tasks;

import org.gradle.api.DefaultTask;

import io.quarkus.gradle.QuarkusPluginExtension;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public abstract class QuarkusTask extends DefaultTask {

    private QuarkusPluginExtension extension;

    QuarkusTask(String description) {
        GradleLogger.logSupplier = this::getLogger;

        setDescription(description);
        setGroup("quarkus");
    }

    QuarkusPluginExtension extension() {
        if (extension == null)
            extension = (QuarkusPluginExtension) getProject().getExtensions().findByName("quarkus");
        return extension;
    }
}
