package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.update.CreatorOutcomeTestBase;

public abstract class ExecutableOutputOutcomeTestBase extends CreatorOutcomeTestBase {

    private static final String LIB_PREFIX = "lib/";
    private static final String MAIN_CLS = "io.quarkus.runner.GeneratedMain";

    protected List<String> expectedLib = new ArrayList<>();

    protected void addToExpectedLib(TsArtifact entry) {
        expectedLib.add(entry.getGroupId() + '.' + entry.getArtifactId() + '-' + entry.getVersion() + '.' + entry.getType());
    }

    protected void assertAppModel(AppModel appModel) throws Exception {
    }

    @Override
    protected void testCreator(QuarkusBootstrap creator) throws Exception {
        System.setProperty("quarkus.package.type", "legacy");
        try {
            CuratedApplication curated = creator.bootstrap();
            assertAppModel(curated.getAppModel());
            AugmentAction action = curated.createAugmentor();
            AugmentResult outcome = action.createProductionApplication();

            final Path libDir = outcome.getJar().getLibraryDir();
            assertTrue(Files.isDirectory(libDir));
            final Set<String> actualLib = new HashSet<>();
            try (Stream<Path> stream = Files.list(libDir)) {
                final Iterator<Path> i = stream.iterator();
                while (i.hasNext()) {
                    actualLib.add(i.next().getFileName().toString());
                }
            }

            final Path runnerJar = outcome.getJar().getPath();
            assertTrue(Files.exists(runnerJar));
            try (JarFile jar = new JarFile(runnerJar.toFile())) {
                final Attributes mainAttrs = jar.getManifest().getMainAttributes();

                // assert the main class
                assertEquals(MAIN_CLS, mainAttrs.getValue("Main-Class"));

                // assert the Class-Path contains all the entries in the lib dir
                final String cp = mainAttrs.getValue("Class-Path");
                assertNotNull(cp);
                String[] cpEntries = Arrays.stream(cp.trim().split("\\s+"))
                        .filter(s -> !s.trim().isEmpty())
                        .toArray(String[]::new);
                assertEquals(actualLib.size(), cpEntries.length);
                for (String entry : cpEntries) {
                    assertTrue(entry.startsWith(LIB_PREFIX));
                    assertTrue(actualLib.contains(entry.substring(LIB_PREFIX.length())));
                }
            }

            List<String> missingEntries = Collections.emptyList();
            for (String entry : expectedLib) {
                if (!actualLib.remove(entry)) {
                    if (missingEntries.isEmpty()) {
                        missingEntries = new ArrayList<>();
                    }
                    missingEntries.add(entry);
                }
            }

            StringBuilder buf = null;
            if (!missingEntries.isEmpty()) {
                buf = new StringBuilder();
                buf.append("Missing entries: ").append(missingEntries.get(0));
                for (int i = 1; i < missingEntries.size(); ++i) {
                    buf.append(", ").append(missingEntries.get(i));
                }
            }
            if (!actualLib.isEmpty()) {
                if (buf == null) {
                    buf = new StringBuilder();
                } else {
                    buf.append("; ");
                }
                final Iterator<String> i = actualLib.iterator();
                buf.append("Extra entries: ").append(i.next());
                while (i.hasNext()) {
                    buf.append(", ").append(i.next());
                }
            }
            if (buf != null) {
                fail(buf.toString());
            }
        } finally {
            System.clearProperty("quarkus.package.type");
        }
    }
}
