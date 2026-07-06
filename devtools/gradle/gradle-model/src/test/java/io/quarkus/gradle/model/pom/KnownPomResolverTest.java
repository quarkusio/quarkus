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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.resolution.UnresolvableModelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.maven.dependency.GAV;

class KnownPomResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldFailClosedForAmbiguousGradleCachePoms() throws IOException {
        installGradleCachePom("first-hash");
        installGradleCachePom("second-hash");
        GAV sample = new GAV("org.acme", "sample", "1.0");
        KnownPomResolver resolver = KnownPomResolver.fromPomClosure(
                Map.of(), Set.of(), List.of(tempDir.toFile()));

        assertThatThrownBy(() -> resolver.resolvePom(sample))
                .isInstanceOf(UnresolvableModelException.class)
                .hasMessageContaining("Could not resolve POM for org.acme:sample:1.0");
    }

    private void installGradleCachePom(String hash) throws IOException {
        Path artifactDirectory = tempDir.resolve("org.acme").resolve("sample").resolve("1.0").resolve(hash);
        Files.createDirectories(artifactDirectory);
        Files.writeString(artifactDirectory.resolve("sample-1.0.pom"), "<project/>", StandardCharsets.UTF_8);
    }
}
