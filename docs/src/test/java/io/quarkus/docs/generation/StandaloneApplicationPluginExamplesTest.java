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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.docs.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

class StandaloneApplicationPluginExamplesTest {

    private static final String FIXTURE_DIRECTORY = "devtools/gradle/gradle-app-plugin/src/test/resources/documentation/"
            + "standalone-application-plugin";
    private static final String MANIFEST_NAME = "gradle-application-plugin-examples.yaml";
    private static final List<String> PUBLIC_GUIDES = List.of(
            "gradle-application-plugin.adoc",
            "aot.adoc");
    private static final Set<String> EXPECTED_TARGETS = Set.of(
            "gradle-application-plugin-basic.gradle",
            "gradle-application-plugin-basic.gradle.kts",
            "gradle-application-plugin-testing.gradle",
            "gradle-application-plugin-testing.gradle.kts",
            "gradle-application-plugin-package-consumer.gradle",
            "gradle-application-plugin-package-consumer.gradle.kts",
            "gradle-application-plugin-package-consumer-settings.gradle",
            "gradle-application-plugin-package-consumer-settings.gradle.kts",
            "gradle-application-plugin-package-producer.gradle",
            "gradle-application-plugin-package-producer.gradle.kts",
            "gradle-application-plugin-advanced.gradle",
            "gradle-application-plugin-advanced.gradle.kts");
    private static final Pattern INCLUDE = Pattern.compile(
            "include::\\{generated-dir}/examples/([^\\[]+)\\[tag=([^,\\]]+)(?:,[^\\]]*)?]");
    private static final Pattern START_TAG = Pattern.compile("(?m)^[ \\t]*// tag::([^\\[]+)\\[][ \\t]*$");
    private static final Pattern END_TAG = Pattern.compile("(?m)^[ \\t]*// end::([^\\[]+)\\[][ \\t]*$");

    @TempDir
    Path tempDir;

    @Test
    void manifestCoversEveryTaggedFixtureAndEveryTargetIsCopied() throws Exception {
        Path repositoryRoot = repositoryRoot();
        Path manifestPath = asciidocSourceDirectory().resolve(MANIFEST_NAME);
        CopyExampleSource.MappingList manifest = readManifest(manifestPath);

        assertEquals(EXPECTED_TARGETS,
                manifest.examples.stream().map(example -> example.target).collect(Collectors.toSet()));
        assertEquals(manifest.examples.size(),
                manifest.examples.stream().map(example -> example.target).distinct().count(),
                "The standalone example manifest must not contain duplicate targets");
        assertEquals(manifest.examples.size(),
                manifest.examples.stream().map(example -> example.source).distinct().count(),
                "Each copied standalone example must have one canonical source");

        Set<String> taggedFixtureSources;
        try (Stream<Path> paths = Files.walk(repositoryRoot.resolve(FIXTURE_DIRECTORY))) {
            taggedFixtureSources = paths.filter(Files::isRegularFile)
                    .filter(StandaloneApplicationPluginExamplesTest::containsTags)
                    .map(repositoryRoot::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .collect(Collectors.toSet());
        }
        assertEquals(taggedFixtureSources,
                manifest.examples.stream().map(example -> example.source).collect(Collectors.toSet()));

        Path manifestDirectory = Files.createDirectory(tempDir.resolve("manifests"));
        Files.copy(manifestPath, manifestDirectory.resolve(MANIFEST_NAME), StandardCopyOption.REPLACE_EXISTING);
        Path output = tempDir.resolve("copied");
        CopyExampleSource copier = new CopyExampleSource();
        copier.outputPath = output;
        copier.rootPath = repositoryRoot;
        copier.srcPaths = List.of(manifestDirectory);
        copier.run();

        for (CopyExampleSource.Example example : manifest.examples) {
            Path copiedTarget = output.resolve(example.target);
            assertTrue(Files.isRegularFile(copiedTarget), () -> "Missing copied target " + copiedTarget);
            String copiedContent = Files.readString(copiedTarget);
            assertFalse(copiedContent.contains("{{source}}"), () -> "Unresolved source marker in " + copiedTarget);
        }
    }

    @Test
    void guideIncludesHaveCompleteAndBalancedTagClosure() throws Exception {
        Path repositoryRoot = repositoryRoot();
        CopyExampleSource.MappingList manifest = readManifest(asciidocSourceDirectory().resolve(MANIFEST_NAME));
        Map<String, CopyExampleSource.Example> examplesByTarget = manifest.examples.stream()
                .collect(Collectors.toMap(example -> example.target, example -> example));
        Map<String, Set<String>> includedTagsByTarget = new HashMap<>();
        for (String guideName : PUBLIC_GUIDES) {
            String guide = Files.readString(asciidocSourceDirectory().resolve(guideName));
            Set<String> guideIncludes = new HashSet<>();
            Matcher includeMatcher = INCLUDE.matcher(guide);
            while (includeMatcher.find()) {
                String target = includeMatcher.group(1);
                String tag = includeMatcher.group(2);
                CopyExampleSource.Example example = examplesByTarget.get(target);
                assertNotNull(example, () -> guideName + " include has no manifest mapping: " + target);
                assertTrue(guideIncludes.add(target + "#" + tag),
                        () -> guideName + " includes the same target and tag more than once: " + target + "#" + tag);
                includedTagsByTarget.computeIfAbsent(target, ignored -> new HashSet<>()).add(tag);
            }
        }
        assertFalse(includedTagsByTarget.isEmpty(), "The public guides must include the tested standalone examples");

        for (CopyExampleSource.Example example : manifest.examples) {
            Path source = repositoryRoot.resolve(example.source);
            String content = Files.readString(source);
            Set<String> tags = balancedTags(content, source);
            assertEquals(tags, includedTagsByTarget.getOrDefault(example.target, Set.of()),
                    () -> "Guide/tag mismatch for " + example.target);
        }

        for (String groovyTarget : EXPECTED_TARGETS) {
            if (groovyTarget.endsWith(".gradle")) {
                String kotlinTarget = groovyTarget + ".kts";
                assertEquals(includedTagsByTarget.get(groovyTarget), includedTagsByTarget.get(kotlinTarget),
                        () -> "Groovy and Kotlin tags differ for " + groovyTarget);
            }
        }
    }

    private static Path repositoryRoot() {
        return CopyExampleSource.docsDir().resolve("..").toAbsolutePath().normalize();
    }

    private static Path asciidocSourceDirectory() {
        return CopyExampleSource.docsDir().resolve("src/main/asciidoc").toAbsolutePath().normalize();
    }

    private static CopyExampleSource.MappingList readManifest(Path manifest) throws Exception {
        YAMLMapper mapper = YAMLMapper.builder(
                YAMLFactory.builder().enable(YAMLWriteFeature.MINIMIZE_QUOTES).build())
                .changeDefaultVisibility(vc -> vc.withFieldVisibility(Visibility.ANY))
                .build();
        return mapper.readValue(manifest.toFile(), CopyExampleSource.MappingList.class);
    }

    private static boolean containsTags(Path path) {
        try {
            return START_TAG.matcher(Files.readString(path)).find();
        } catch (IOException e) {
            throw new IllegalStateException("Could not inspect example source " + path, e);
        }
    }

    private static Set<String> balancedTags(String content, Path source) {
        List<String> tags = new ArrayList<>();
        Deque<String> openTags = new ArrayDeque<>();
        for (String line : content.lines().toList()) {
            Matcher start = START_TAG.matcher(line);
            if (start.matches()) {
                tags.add(start.group(1));
                openTags.push(start.group(1));
                continue;
            }
            Matcher end = END_TAG.matcher(line);
            if (end.matches()) {
                assertFalse(openTags.isEmpty(), () -> "End tag without start in " + source + ": " + end.group(1));
                assertEquals(openTags.pop(), end.group(1),
                        () -> "Crossed or mismatched tags in " + source + " at " + end.group(1));
            }
        }
        assertEquals(tags.size(), new HashSet<>(tags).size(), () -> "Duplicate tags in " + source + ": " + tags);
        assertTrue(openTags.isEmpty(), () -> "Unclosed tags in " + source + ": " + openTags);
        return Set.copyOf(tags);
    }
}
