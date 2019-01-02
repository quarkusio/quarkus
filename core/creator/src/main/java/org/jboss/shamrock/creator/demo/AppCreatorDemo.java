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

package org.jboss.shamrock.creator.demo;

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

import org.jboss.shamrock.creator.AppCreator;
import org.jboss.shamrock.creator.phase.augment.AugmentPhase;
import org.jboss.shamrock.creator.phase.curate.PomStateCreationPhase;
import org.jboss.shamrock.creator.phase.curate.PomStateReadingPhase;
import org.jboss.shamrock.creator.phase.nativeimage.NativeImageOutcome;
import org.jboss.shamrock.creator.phase.nativeimage.NativeImagePhase;
import org.jboss.shamrock.creator.phase.runnerjar.RunnerJarOutcome;
import org.jboss.shamrock.creator.util.IoUtils;
import org.jboss.shamrock.creator.util.PropertyUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class AppCreatorDemo {

    /**
     * This demo assumes you've built shamrock and its artifacts are in your local repo
     * (otherwise they would have to be available in one of the remote repos to be resolvable)
     *
     * AND also example shamrock-strict-bean-validation-example-1.0.0.Alpha1-SNAPSHOT.jar. This jar is
     * used as an example user app that is augmented and turned into a runnable application
     */

    public static void main(String[] args) throws Exception {
        final Path shamrockRoot = Paths.get("").toAbsolutePath().getParent().getParent();
        final Path exampleTarget = shamrockRoot.resolve("integration-tests").resolve("bean-validation-strict").resolve("target");

        final Path appJar = exampleTarget.resolve("shamrock-integration-test-bean-validation-1.0.0.Alpha1-SNAPSHOT.jar");
        if (!Files.exists(appJar)) {
            throw new Exception("Failed to locate user app " + appJar);
        }

        final Path demoDir = Paths.get(PropertyUtils.getUserHome()).resolve("shamrock-creator-demo");
        IoUtils.recursiveDelete(demoDir);

        buildRunnableJar(appJar, demoDir);
        //buildNativeImage(appJar, demoDir);
        //curateRunnableJar(appJar, demoDir);

        logLibDiff(exampleTarget, demoDir);
    }

    private static void curateRunnableJar(Path userApp, Path outputDir) throws Exception {

        try (AppCreator appCreator = AppCreator.builder()
                .addPhase(new PomStateCreationPhase())
                .addPhase(new PomStateReadingPhase())
                .setAppJar(userApp)
                .setWorkDir(outputDir)
                .build()) {
            appCreator.resolveOutcome(PomStateReadingPhase.Outcome.class);
        }
/*
        new AppCreator()

        // enabling debug allows to see resolved application dependencies in the terminal
        .setDebug(true)

        // setting a work dir can be useful if you want to see the temporary content used
        // by various phases during app building
        .setWorkDir(outputDir)

        .addPhase(new PomStateCreationPhase())
        .addPhase(new PomStateReadingPhase())
        //.addPhase(new CuratePhase())
        //.addPhase(new AugmentPhase().setOutputDir(outputDir))
        .create(userApp);
        */
    }

    private static void buildRunnableJar(Path userApp, Path outputDir) throws Exception {

        final RunnerJarOutcome runnerJar;
        try (AppCreator appCreator = AppCreator.builder()
                .addPhase(new AugmentPhase()/*.setOutputDir(outputDir)*/)
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
                .addPhase(new AugmentPhase())
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
        if(names.isEmpty()) {
            return;
        }
        System.out.println(header);
        final List<String> sorted = new ArrayList<>(names);
        Collections.sort(sorted);
        for(int i = 0; i < sorted.size(); ++i) {
            System.out.println((i + 1) + ") " + sorted.get(i));
        }
    }

    private static Set<String> readNames(Path path) throws IOException {
        Set<String> names = new HashSet<>();
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for(Path p : stream) {
                names.add(p.getFileName().toString());
            }
        }
        return names;
    }
}
