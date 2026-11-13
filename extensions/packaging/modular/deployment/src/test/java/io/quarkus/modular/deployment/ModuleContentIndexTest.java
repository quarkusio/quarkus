package io.quarkus.modular.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.FilteredPathTree;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathFilter;
import io.quarkus.paths.PathTree;
import io.quarkus.paths.PathVisit;
import io.quarkus.paths.PathVisitor;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.attribute.ModuleAttribute;
import io.smallrye.classfile.attribute.ModulePackagesAttribute;
import io.smallrye.classfile.extras.constant.ModuleDesc;
import io.smallrye.classfile.extras.constant.PackageDesc;

class ModuleContentIndexTest {
    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void batchesExactClassLookupsAndReusesPackageInventory(boolean packageAttribute) throws IOException {
        Set<String> packages = Set.of("shared.p0", "shared.p1", "shared.p2", "shared.p3", "shared.p4");
        List<Artifact> artifacts = new ArrayList<>();
        Map<ResolvedDependency, ClassModel> descriptors = new HashMap<>();
        Map<String, Set<String>> requested = new HashMap<>();
        Set<String> expectedLookups = new HashSet<>();
        for (int artifact = 0; artifact < 4; artifact++) {
            List<String> entries = new ArrayList<>();
            for (int type = 0; type < 250; type++) {
                entries.add("shared/p" + type % 5 + "/Artifact" + artifact + "Class" + type + ".class");
            }
            Artifact fixture = artifact("module" + artifact, entries, packageAttribute ? packages : null, false);
            artifacts.add(fixture);
            descriptors.put(fixture.dependency(), fixture.descriptor());
            for (int type = 0; type < 2; type++) {
                String name = "shared.p" + type + ".Artifact" + artifact + "Class" + type;
                requested.computeIfAbsent("shared.p" + type, ignored -> new HashSet<>()).add(name);
                expectedLookups.add(name.replace('.', '/') + ".class");
            }
        }
        requested.get("shared.p0").add("shared.p0.Missing");
        expectedLookups.add("shared/p0/Missing.class");
        requested.put("absent", Set.of("absent.Missing"));
        Artifact unrelated = artifact("unrelated", List.of("unrelated/Other.class"), Set.of("unrelated"), false);
        descriptors.put(unrelated.dependency(), unrelated.descriptor());
        var index = new ModuleContentIndex(descriptors);

        for (int artifact = 0; artifact < artifacts.size(); artifact++) {
            Artifact fixture = artifacts.get(artifact);
            // The package is shared across all four JARs; only exact class membership establishes ownership.
            assertThat(index.findClasses(fixture.dependency(), requested)).containsExactlyInAnyOrder(
                    "shared.p0.Artifact" + artifact + "Class0", "shared.p1.Artifact" + artifact + "Class1");
            assertThat(index.packages(fixture.dependency())).containsExactlyInAnyOrderElementsOf(packages);
            assertThat(index.packages(fixture.dependency())).containsExactlyInAnyOrderElementsOf(packages);
            assertThat(fixture.tree().opens).as("opens for artifact %s", artifact).isEqualTo(1);
            assertThat(fixture.tree().closes).as("closed views for artifact %s", artifact).isEqualTo(1);
            assertThat(fixture.tree().walks).as("package walks for artifact %s", artifact)
                    .isEqualTo(packageAttribute ? 0 : 1);
            assertThat(fixture.tree().lookups).containsExactlyInAnyOrderElementsOf(expectedLookups);
        }
        assertThat(index.findClasses(unrelated.dependency(), requested)).isEmpty();
        assertThat(index.packages(unrelated.dependency())).containsExactly("unrelated");
        assertThat(unrelated.tree().opens).isZero();
        assertThat(unrelated.tree().walks).isZero();
        assertThat(unrelated.tree().lookups).isEmpty();
    }

    @Test
    void packageDiscoveryWithoutClassRequestsIsCached() throws IOException {
        Artifact fixture = artifact("automatic", List.of("sample/One.class", "sample/Two.class", "DefaultClass.class",
                "assets/config.properties"), null, false);
        var index = new ModuleContentIndex(Map.of());
        assertThat(index.packages(fixture.dependency())).containsExactly("sample");
        assertThat(index.packages(fixture.dependency())).containsExactly("sample");
        assertThat(fixture.tree().opens).isEqualTo(1);
        assertThat(fixture.tree().closes).isEqualTo(1);
        assertThat(fixture.tree().walks).isEqualTo(1);
        assertThat(fixture.tree().lookups).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void exactLookupUsesCurrentMultiReleaseClassView(boolean packageAttribute) throws IOException {
        Artifact fixture = artifact("multirelease", List.of("sample/Base.class",
                "META-INF/versions/9/sample/Versioned.class",
                "META-INF/versions/" + (Runtime.version().feature() + 1) + "/sample/Future.class"),
                packageAttribute ? Set.of("sample") : null, true);
        var index = new ModuleContentIndex(Map.of(fixture.dependency(), fixture.descriptor()));
        assertThat(index.findClasses(fixture.dependency(), Map.of("sample",
                Set.of("sample.Base", "sample.Versioned", "sample.Future", "sample.Missing"))))
                .containsExactlyInAnyOrder("sample.Base", "sample.Versioned");
        assertThat(index.packages(fixture.dependency())).containsExactly("sample");
        assertThat(fixture.tree().opens).isEqualTo(1);
        assertThat(fixture.tree().closes).isEqualTo(1);
        assertThat(fixture.tree().walks).isEqualTo(packageAttribute ? 0 : 1);
        assertThat(fixture.tree().lookups).containsExactlyInAnyOrder("sample/Base.class", "sample/Versioned.class",
                "sample/Future.class", "sample/Missing.class");
    }

    private Artifact artifact(String module, List<String> entries, Set<String> packages, boolean multiRelease)
            throws IOException {
        byte[] descriptor = ClassFile.of().buildModule(ModuleAttribute.of(ModuleDesc.of(module), builder -> {
        }), builder -> {
            if (packages != null) {
                builder.with(ModulePackagesAttribute.ofNames(packages.stream().map(PackageDesc::of).toList()));
            }
        });
        Path jar = directory.resolve(module + ".jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (multiRelease) {
            manifest.getMainAttributes().put(Attributes.Name.MULTI_RELEASE, "true");
        }
        try (var output = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            output.putNextEntry(new JarEntry("module-info.class"));
            output.write(descriptor);
            output.closeEntry();
            for (String entry : entries) {
                output.putNextEntry(new JarEntry(entry));
                output.closeEntry();
            }
        }
        var tree = new CountingPathTree(PathTree.ofArchive(jar));
        ResolvedDependency dependency = new ResolvedDependencyBuilder() {
            @Override
            public PathTree getContentTree() {
                return tree;
            }

            @Override
            public String getModuleName() {
                return module;
            }
        }.setGroupId("test").setArtifactId(module).setVersion("1").setResolvedPath(jar);
        return new Artifact(dependency, tree, ClassFile.of().parse(descriptor));
    }

    private record Artifact(ResolvedDependency dependency, CountingPathTree tree, ClassModel descriptor) {
    }

    /** Counts real archive sessions, including implicit opens from operations on the closed tree. */
    private static final class CountingPathTree extends FilteredPathTree {
        private final PathTree delegate;
        private int opens;
        private int closes;
        private int walks;
        private final List<String> lookups = new ArrayList<>();

        private CountingPathTree(PathTree delegate) {
            super(delegate, new PathFilter(List.of(), List.of()));
            this.delegate = delegate;
        }

        @Override
        public OpenPathTree open() {
            opens++;
            return new CountingOpenPathTree(delegate.open());
        }

        private <T> T withOpen(Function<OpenPathTree, T> operation) {
            try (var tree = open()) {
                return operation.apply(tree);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void walk(PathVisitor visitor) {
            withOpen(tree -> {
                tree.walk(visitor);
                return null;
            });
        }

        @Override
        public boolean contains(String name) {
            return withOpen(tree -> tree.contains(name));
        }

        @Override
        public <T> T apply(String name, Function<PathVisit, T> operation) {
            return withOpen(tree -> tree.apply(name, operation));
        }

        @Override
        public void accept(String name, Consumer<PathVisit> operation) {
            withOpen(tree -> {
                tree.accept(name, operation);
                return null;
            });
        }

        private final class CountingOpenPathTree extends FilteredPathTree implements OpenPathTree {
            private final OpenPathTree delegate;

            private CountingOpenPathTree(OpenPathTree delegate) {
                super(delegate, new PathFilter(List.of(), List.of()));
                this.delegate = delegate;
            }

            @Override
            public void walk(PathVisitor visitor) {
                walks++;
                super.walk(visitor);
            }

            @Override
            public boolean contains(String name) {
                lookups.add(name);
                return super.contains(name);
            }

            @Override
            public PathTree getOriginalTree() {
                return CountingPathTree.this;
            }

            @Override
            public boolean isOpen() {
                return delegate.isOpen();
            }

            @Override
            public Path getPath(String name) {
                return delegate.getPath(name);
            }

            @Override
            public void close() throws IOException {
                closes++;
                delegate.close();
            }
        }
    }
}
