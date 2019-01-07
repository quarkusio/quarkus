/*
 *
 *   Copyright (c) 2016-2018 Red Hat, Inc.
 *
 *   Red Hat licenses this file to you under the Apache License, version
 *   2.0 (the "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *   implied.  See the License for the specific language governing
 *   permissions and limitations under the License.
 */
package org.jboss.shamrock.maven.components.dependencies;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.jboss.shamrock.maven.CreateProjectMojo;
import org.jboss.shamrock.maven.utilities.MojoUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class Extensions {

    private Extensions() {
        // avoid direct instantiation
    }

    public static List<Extension> get() {
        ObjectMapper mapper = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);
        URL url = Extensions.class.getClassLoader().getResource("extensions.json");
        try {
            return mapper.readValue(url, new TypeReference<List<Extension>>() {
                // Do nothing.
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to load the extensions.json file", e);
        }
    }

    public static Dependency parse(String dependency, Log log) {
        Dependency res = new Dependency();
        String[] segments = dependency.split(":");
        if (segments.length >= 2) {
            res.setGroupId(segments[0]);
            res.setArtifactId(segments[1]);
            if (segments.length >= 3 && !segments[2].isEmpty()) {
                res.setVersion(segments[2]);
            }
            if (segments.length >= 4) {
                res.setClassifier(segments[3]);
            }
            return res;
        } else {
            log.warn("Invalid dependency description '" + dependency + "'");
            return null;
        }
    }

    public static boolean addExtensions(Model model, List<String> extensions, Log log) {
        if (extensions == null || extensions.isEmpty()) {
            return false;
        }

        boolean updated = false;
        List<Extension> exts = Extensions.get();
        for (String dependency : extensions) {
            Optional<Extension> optional = exts.stream()
                    .filter(d -> {
                        boolean hasTag = d.labels().contains(dependency.trim().toLowerCase());
                        boolean machName = d.getName().toLowerCase().contains(dependency.trim().toLowerCase());
                        return hasTag || machName;
                    })
                    .findAny();

            if (optional.isPresent()) {
                if (!MojoUtils.hasDependency(model, optional.get().getGroupId(), optional.get().getArtifactId())) {
                    log.info("Adding extension " + optional.get().toCoordinates());

                    if (containsBOM(model)  && optional.get().getGroupId().startsWith(CreateProjectMojo.PLUGIN_GROUPID)) {
                        model.addDependency(optional.get().toDependency(true));
                    } else {
                        model.addDependency(optional.get().toDependency(false));
                    }

                    updated = true;
                } else {
                    log.info("Extension already present - skipping");
                }

            } else if (dependency.contains(":")) {
                // Add it as a dependency
                // groupId:artifactId:version:classifier
                Dependency parsed = Extensions.parse(dependency, log);
                if (parsed != null) {
                    log.info("Adding dependency " + parsed.getManagementKey());
                    model.addDependency(parsed);
                    updated = true;
                }
            } else {
                log.warn("Cannot find a dependency matching '" + dependency + "'");
            }
        }

        return updated;
    }

    private static boolean containsBOM(Model model) {
        List<Dependency> dependencies = model.getDependencyManagement().getDependencies();
        return dependencies.stream()
                // Find bom
                .filter(dependency -> "import".equalsIgnoreCase(dependency.getScope()))
                .filter(dependency -> "pom".equalsIgnoreCase(dependency.getType()))
                // Does it matches the bom artifact name
                .anyMatch(dependency -> dependency.getArtifactId().equalsIgnoreCase(MojoUtils.get("bom-artifactId")));
    }

}
