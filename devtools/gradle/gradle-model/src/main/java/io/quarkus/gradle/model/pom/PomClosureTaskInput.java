/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.quarkus.gradle.model.pom;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import io.quarkus.maven.dependency.GAV;

@SuppressWarnings("ClassCanBeRecord") // Gradle doesn't like records in this case
public final class PomClosureTaskInput implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, String> resolvedPomFilesByGav;
    private final List<String> missingPomGavs;
    private final List<File> resolvedPomFiles;

    private PomClosureTaskInput(Map<String, String> resolvedPomFilesByGav, List<String> missingPomGavs,
            List<File> resolvedPomFiles) {
        this.resolvedPomFilesByGav = Map.copyOf(resolvedPomFilesByGav);
        this.missingPomGavs = List.copyOf(missingPomGavs);
        this.resolvedPomFiles = List.copyOf(resolvedPomFiles);
    }

    public static PomClosureTaskInput from(PomClosureResult result) {
        Map<String, String> resolved = new TreeMap<>();
        result.resolvedPoms()
                .forEach((gav, file) -> resolved.put(gav.toString(), file.getAbsolutePath()));
        List<String> missing = result.missingPoms().stream()
                .map(GAV::toString)
                .sorted()
                .toList();
        List<File> files = resolved.values().stream()
                .map(File::new)
                .toList();
        return new PomClosureTaskInput(resolved, missing, files);
    }

    @Input
    public Map<String, String> getResolvedPomFilesByGav() {
        return resolvedPomFilesByGav;
    }

    @Input
    public List<String> getMissingPomGavs() {
        return missingPomGavs;
    }

    @Classpath
    public List<File> getResolvedPomFiles() {
        return resolvedPomFiles;
    }

    @Internal
    public PomClosureResult getResult() {
        Map<GAV, File> resolved = new TreeMap<>((left, right) -> left.toString().compareTo(right.toString()));
        resolvedPomFilesByGav.forEach((gav, file) -> resolved.put(parseGav(gav), new File(file)));
        Set<GAV> missing = new TreeSet<>((left, right) -> left.toString().compareTo(right.toString()));
        missingPomGavs.stream().map(PomClosureTaskInput::parseGav).forEach(missing::add);
        return new PomClosureResult(resolved, missing);
    }

    private static GAV parseGav(String value) {
        String[] parts = value.split(":", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("POM closure GAV must have format groupId:artifactId:version: " + value);
        }
        return new GAV(parts[0], parts[1], parts[2]);
    }
}
