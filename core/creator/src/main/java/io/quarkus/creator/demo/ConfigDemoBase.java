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
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import io.quarkus.creator.AppCreator;
import io.quarkus.creator.config.reader.PropertiesConfigReader;
import io.quarkus.creator.config.reader.PropertiesHandler;
import io.quarkus.creator.phase.curate.CuratePhase;
import io.quarkus.creator.util.IoUtils;
import io.quarkus.creator.util.PropertyUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigDemoBase {

    protected Path appJar;
    protected Path workDir;

    public ConfigDemoBase() {
    }

    public void run() throws Exception {

        final long startTime = System.currentTimeMillis();

        final Path appJar = getAppJar();
        if (!Files.exists(appJar)) {
            throw new IllegalStateException("Failed to locate user app " + appJar);
        }

        final Path demoDir = getDemoWorkDir();
        IoUtils.recursiveDelete(demoDir);

        final Properties props = getProperties();
        Files.createDirectories(demoDir);
        final Path propsFile = demoDir.resolve("app-creator.properties");
        try (OutputStream out = Files.newOutputStream(propsFile)) {
            props.store(out, "Example AppCreator properties");
        }

        final PropertiesHandler<AppCreator> propsHandler = AppCreator.builder()
                .setAppJar(appJar)
                .getPropertiesHandler();
        try (final AppCreator appCreator = PropertiesConfigReader.getInstance(propsHandler).read(propsFile)) {
            demo(appCreator);
            if (isLogLibDiff()) {
                logLibDiff(appJar.getParent(), demoDir);
            }
        }

        final long time = System.currentTimeMillis() - startTime;
        final long seconds = time / 1000;
        System.out.println("Done in " + seconds + "." + (time - seconds * 1000) + " seconds");
    }

    protected void demo(AppCreator creator) throws Exception {
    }

    protected boolean isLogLibDiff() {
        return false;
    }

    protected Path initAppJar() {
        final Path quarkusRoot = Paths.get("").toAbsolutePath().getParent().getParent();
        //final Path appDir = quarkusRoot.resolve("integration-tests").resolve("bean-validation-strict").resolve("target");
        //final Path appJar = appDir.resolve("quarkus-integration-test-bean-validation-1.0.0.Alpha1-SNAPSHOT.jar");

        final Path quickstartsRoot = quarkusRoot.getParent().resolve("quarkus-quickstarts");
        if (!Files.exists(quickstartsRoot)) {
            throw new IllegalStateException("Failed to locate quarkus-quickstarts repo at " + quickstartsRoot);
        }
        final Path appDir = quickstartsRoot.resolve("application-configuration").resolve("target");
        final Path appJar = appDir.resolve("application-configuration-1.0-SNAPSHOT.jar");
        return appJar;
    }

    public Path getAppJar() {
        return appJar == null ? appJar = initAppJar() : appJar;
    }

    public Path getDemoWorkDir() {
        return workDir == null ? workDir = Paths.get(PropertyUtils.getUserHome()).resolve("quarkus-creator-demo") : workDir;
    }

    public Properties getProperties() {
        final Properties props = new Properties();
        final Path demoDir = getDemoWorkDir();
        if (demoDir != null) {
            props.setProperty("output", demoDir.toString());
        }
        props.setProperty(CuratePhase.completePropertyName(CuratePhase.CONFIG_PROP_LOCAL_REPO),
                Paths.get(PropertyUtils.getUserHome(), "quarkus-curate-repo").toString());
        initProps(props);
        return props;
    }

    protected void initProps(Properties props) {
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
