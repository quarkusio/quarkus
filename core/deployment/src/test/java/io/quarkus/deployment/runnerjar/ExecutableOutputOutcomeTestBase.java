package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsArtifact.ContentProvider;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.update.CreatorOutcomeTestBase;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;

public abstract class ExecutableOutputOutcomeTestBase extends CreatorOutcomeTestBase {

    private static final String LIB_PREFIX = "lib/";
    private static final String MAIN_CLS = "io.quarkus.runner.GeneratedMain";

    protected List<String> expectedLib = new ArrayList<>();
    protected TsDependency platformDescriptor;
    protected TsDependency platformPropsDep;

    protected void addToExpectedLib(TsArtifact entry) {
        expectedLib.add(entry.getGroupId() + '.' + entry.getArtifactId() + '-' + entry.getVersion() + '.' + entry.getType());
    }

    protected void assertDeploymentDeps(Set<Dependency> deploymentDeps) throws Exception {
    }

    protected void assertAppModel(ApplicationModel appModel) throws Exception {
    }

    protected String[] expectedExtensionDependencies() {
        return null;
    }

    protected TsDependency platformDescriptor() {
        if (platformDescriptor == null) {
            TsArtifact platformDescr = new TsArtifact("org.acme",
                    "acme" + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX, "1.0", "json",
                    "1.0");
            platformDescr.setContent(new ContentProvider() {
                Path platformJson;

                @Override
                public Path getPath(Path workDir) throws IOException {
                    if (platformJson == null) {
                        platformJson = workDir.resolve("platform-descriptor.json");
                        try (BufferedWriter writer = Files.newBufferedWriter(platformJson)) {
                            writer.write("platform descriptor");
                        }
                    }
                    return platformJson;
                }
            });
            platformDescr.install(repo);
            platformDescriptor = new TsDependency(platformDescr);
        }
        return platformDescriptor;
    }

    protected TsDependency platformProperties() {
        if (platformPropsDep == null) {
            TsArtifact platformProps = new TsArtifact("org.acme",
                    "acme" + BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX, null, "properties",
                    "1.0");
            platformProps.setContent(new ContentProvider() {
                Path propsFile;

                @Override
                public Path getPath(Path workDir) throws IOException {
                    if (propsFile == null) {
                        Properties props = new Properties();
                        props.setProperty("platform.quarkus.native.builder-image", "builder-image-url");
                        propsFile = workDir.resolve("platform-properties.properties");
                        try (OutputStream os = Files.newOutputStream(propsFile)) {
                            props.store(os, "Test Quarkus platform properties");
                        }
                    }
                    return propsFile;
                }
            });
            platformProps.install(repo);
            platformPropsDep = new TsDependency(platformProps);
        }
        return platformPropsDep;
    }

    @Override
    protected void testCreator(QuarkusBootstrap creator) throws Exception {
        System.setProperty("quarkus.package.type", "legacy-jar");
        try {
            CuratedApplication curated = creator.bootstrap();
            assertAppModel(curated.getApplicationModel());
            final String[] expectedExtensions = expectedExtensionDependencies();
            if (expectedExtensions != null) {
                assertExtensionDependencies(curated.getApplicationModel(), expectedExtensions);
            }
            assertDeploymentDeps(
                    curated.getApplicationModel().getDependencies().stream().filter(d -> d.isDeploymentCp() && !d.isRuntimeCp())
                            .map(d -> new ArtifactDependency(d)).collect(Collectors.toSet()));
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

    private static void assertExtensionDependencies(ApplicationModel appModel, String[] expectedExtensions) {
        final Set<String> expectedRuntime = new HashSet<>(expectedExtensions.length);
        final Set<String> expectedDeployment = new HashSet<>(expectedExtensions.length);
        for (String rtId : expectedExtensions) {
            expectedRuntime.add(TsArtifact.DEFAULT_GROUP_ID + ":" + rtId + "::jar:" + TsArtifact.DEFAULT_VERSION);
            expectedDeployment
                    .add(TsArtifact.DEFAULT_GROUP_ID + ":" + rtId + "-deployment" + "::jar:" + TsArtifact.DEFAULT_VERSION);
        }

        final Collection<ResolvedDependency> rtDeps = appModel.getRuntimeDependencies();
        for (Dependency dep : rtDeps) {
            final String coords = dep.toGACTVString();
            assertTrue(expectedRuntime.contains(coords), coords);
        }
        assertEquals(expectedExtensions.length, rtDeps.size());

        final List<Dependency> deploymentOnly = appModel.getDependencies().stream()
                .filter(d -> d.isDeploymentCp() && !d.isRuntimeCp()).collect(Collectors.toList());
        for (Dependency dep : deploymentOnly) {
            final String coords = dep.toGACTVString();
            assertTrue(expectedDeployment.contains(coords), coords);
        }
        assertEquals(expectedExtensions.length, deploymentOnly.size());
    }
}
