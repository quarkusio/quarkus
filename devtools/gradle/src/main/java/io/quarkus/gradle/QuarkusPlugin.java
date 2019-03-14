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
package io.quarkus.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.util.GradleVersion;

import io.quarkus.gradle.tasks.QuarkusAddExtension;
import io.quarkus.gradle.tasks.QuarkusBuild;
import io.quarkus.gradle.tasks.QuarkusDev;
import io.quarkus.gradle.tasks.QuarkusListExtensions;
import io.quarkus.gradle.tasks.QuarkusNative;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class QuarkusPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        verifyGradleVersion();
        //register extension
        project.getExtensions().create("quarkus", QuarkusPluginExtension.class, project);

        registerListExtensions(project);
    }

    private void registerListExtensions(Project project) {
        project.getTasks().create("listExtensions", QuarkusListExtensions.class);
        project.getTasks().create("addExtension", QuarkusAddExtension.class);
        project.getTasks().create("quarkusDev", QuarkusDev.class).dependsOn("build");
        project.getTasks().create("quarkusBuild", QuarkusBuild.class).dependsOn("build");
        project.getTasks()
                .create("quarkusNative", QuarkusNative.class)
                .dependsOn("quarkusBuild");
    }

    private void verifyGradleVersion() {
        if (GradleVersion.current().compareTo(GradleVersion.version("5.0")) < 0) {
            throw new GradleException("Quarkus plugin requires Gradle 5.0 or later. Current version is: " +
                    GradleVersion.current());
        }
    }
}
