/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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

package io.quarkus.creator.demo;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.creator.AppCreator;
import io.quarkus.creator.phase.augment.AugmentPhase;
import io.quarkus.creator.phase.curate.CuratePhase;
import io.quarkus.creator.phase.nativeimage.NativeImageOutcome;
import io.quarkus.creator.phase.nativeimage.NativeImagePhase;
import io.quarkus.creator.phase.runnerjar.RunnerJarOutcome;
import io.quarkus.creator.phase.runnerjar.RunnerJarPhase;
import io.quarkus.creator.util.IoUtils;
import io.quarkus.creator.util.PropertyUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class AppCreatorDemo {

    /**
     * This demo assumes you've built quarkus and its artifacts are in your local repo
     * (otherwise they would have to be available in one of the remote repos to be resolvable)
     *
     * AND also example quarkus-strict-bean-validation-example-1.0.0.Alpha1-SNAPSHOT.jar. This jar is
     * used as an example user app that is augmented and turned into a runnable application
     */

    public static void main(String[] args) throws Exception {
        final Path quarkusRoot = Paths.get("").toAbsolutePath().getParent().getParent();
        final Path exampleTarget = quarkusRoot.resolve("integration-tests").resolve("bean-validation-strict").resolve("target");

        final Path appJar = exampleTarget.resolve("quarkus-integration-test-bean-validation-1.0.0.Alpha1-SNAPSHOT.jar");
        if (!Files.exists(appJar)) {
            throw new Exception("Failed to locate user app " + appJar);
        }

        final Path demoDir = Paths.get(PropertyUtils.getUserHome()).resolve("quarkus-creator-demo");
        IoUtils.recursiveDelete(demoDir);

        buildRunnableJar(appJar, demoDir);
        //buildNativeImage(appJar, demoDir);
        //curateRunnableJar(appJar, demoDir);

        //logLibDiff(exampleTarget, demoDir);
    }

    private static void buildRunnableJar(Path userApp, Path outputDir) throws Exception {

        final RunnerJarOutcome runnerJar;
        try (AppCreator appCreator = AppCreator.builder()
                .addPhase(new CuratePhase())
                .addPhase(new AugmentPhase()/* .setOutputDir(outputDir) */)
                .addPhase(new RunnerJarPhase()/* .setOutputDir(outputDir) */)
                .addPhase(new NativeImagePhase())
                .setAppJar(userApp)
                .build()) {
            runnerJar = appCreator.resolveOutcome(RunnerJarOutcome.class);
            System.out.println("Runner JAR: " + runnerJar.getRunnerJar() + " exists=" + Files.exists(runnerJar.getRunnerJar()));
        }
        System.out.println("Runner JAR: " + runnerJar.getRunnerJar() + " exists=" + Files.exists(runnerJar.getRunnerJar()));
    }

    private static void buildNativeImage(Path userApp, Path outputDir) throws Exception {

        try (AppCreator appCreator = AppCreator.builder()
                //.setOutput(outputDir)
                .addPhase(new CuratePhase())
                .addPhase(new AugmentPhase())
                .addPhase(new RunnerJarPhase())
                .addPhase(new NativeImagePhase().setOutputDir(outputDir))
                .setAppJar(userApp)
                .build()) {
            appCreator.resolveOutcome(NativeImageOutcome.class);
        }
    }

    private static void logLibDiff(final Path exampleTarget, final Path testBuildDir) throws IOException {
        final Set<String> originalNames = readNames(exampleTarget.resolve("lib"));
        final Set<String> aetherNames = readNames(testBuildDir.resolve("lib"));

        final Set<String> originalOnly = new HashSet<>(originalNames);
        originalOnly.removeAll(aetherNames);
        logNames("Original build lib jars not found in the test lib:", originalOnly);

        final Set<String> aetherOnly = new HashSet<>(aetherNames);
        aetherOnly.removeAll(originalNames);
        logNames("Test lib jars not found in the original build lib:", aetherOnly);
    }

    private static void logNames(String header, Set<String> names) {
        if (names.isEmpty()) {
            return;
        }
        System.out.println(header);
        final List<String> sorted = new ArrayList<>(names);
        Collections.sort(sorted);
        for (int i = 0; i < sorted.size(); ++i) {
            System.out.println((i + 1) + ") " + sorted.get(i));
        }
    }

    private static Set<String> readNames(Path path) throws IOException {
        Set<String> names = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path p : stream) {
                names.add(p.getFileName().toString());
            }
        }
        return names;
    }
}
