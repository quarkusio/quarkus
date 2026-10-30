package io.quarkus.deployment.pkg.steps;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.deployment.pkg.TreeShakeConfig;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.smallrye.classfile.Annotation;
import io.smallrye.classfile.AnnotationElement;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.attribute.RuntimeVisibleAnnotationsAttribute;

/**
 * Builds {@link JarTreeShakeInput} instances from in-memory bytecode for tree-shaker tests.
 */
final class TreeShakeInputFixture {

    private final Map<String, Supplier<byte[]>> depBytecode = new HashMap<>();
    private final Map<String, Supplier<byte[]>> appBytecode = new HashMap<>();
    private final Map<String, ArtifactKey> classToDep = new HashMap<>();
    private final Set<String> roots = new HashSet<>();
    private final Map<String, Set<String>> conditionalRoots = new HashMap<>();

    private TreeShakeInputFixture() {
    }

    static TreeShakeInputFixture builder() {
        return new TreeShakeInputFixture();
    }

    /**
     * Adds classes of a dependency identified by the given artifact key (e.g. {@code org.acme:lib}).
     */
    TreeShakeInputFixture dependency(String artifactKey, byte[]... classes) {
        final ArtifactKey key = ArtifactKey.fromString(artifactKey);
        for (byte[] bytecode : classes) {
            final String name = className(bytecode);
            depBytecode.put(name, () -> bytecode);
            classToDep.put(name, key);
        }
        return this;
    }

    /**
     * Adds application classes, each of which becomes a root.
     */
    TreeShakeInputFixture application(byte[]... classes) {
        for (byte[] bytecode : classes) {
            final String name = className(bytecode);
            appBytecode.put(name, () -> bytecode);
            roots.add(name);
        }
        return this;
    }

    TreeShakeInputFixture root(String className) {
        roots.add(className);
        return this;
    }

    TreeShakeInputFixture conditionalRoot(String condition, String className) {
        conditionalRoots.computeIfAbsent(condition, k -> new HashSet<>()).add(className);
        return this;
    }

    JarTreeShakeInput build() {
        return new JarTreeShakeInput(
                TreeShakeConfig.TreeShakeMode.CLASSES,
                new HashSet<>(roots),
                new HashMap<>(conditionalRoots),
                new HashSet<>(),
                new HashMap<>(depBytecode),
                new HashMap<>(appBytecode),
                new HashMap<>(),
                new HashMap<>(classToDep),
                new HashMap<>(),
                new HashMap<>(),
                new HashSet<>(depBytecode.keySet()),
                Runtime.version().feature(),
                new HashMap<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new HashSet<>(),
                new HashMap<>(),
                new HashSet<>());
    }

    /**
     * Runs the tree shaker on the given input and returns the names of the dependency classes it keeps.
     */
    static Set<String> keptDependencyClasses(JarTreeShakeInput input) {
        final Set<String> kept = new HashSet<>(input.depBytecode.keySet());
        final JarTreeShakeBuildItem result = new JarTreeShaker(input).run();
        for (List<String> removed : result.getRemovedClasses().values()) {
            for (String classFile : removed) {
                kept.remove(classFile.substring(0, classFile.length() - ".class".length()).replace('/', '.'));
            }
        }
        return kept;
    }

    /**
     * Generates a class whose static method loads a class literal of each of the referenced classes.
     */
    static byte[] classReferencing(String name, String... referencedClasses) {
        return ClassFile.of().build(ClassDesc.of(name), classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_PUBLIC);
            classBuilder.withMethodBody("refs", MethodTypeDesc.of(ConstantDescs.CD_void),
                    ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC, code -> {
                        for (String referenced : referencedClasses) {
                            code.loadConstant(ClassDesc.of(referenced));
                            code.pop();
                        }
                        code.return_();
                    });
        });
    }

    /**
     * Generates a class annotated with a runtime-visible annotation whose {@code value} element is a class literal.
     */
    static byte[] classAnnotatedWith(String name, String annotationType, String classLiteralValue) {
        return ClassFile.of().build(ClassDesc.of(name), classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_PUBLIC);
            classBuilder.with(RuntimeVisibleAnnotationsAttribute.of(Annotation.of(ClassDesc.of(annotationType),
                    AnnotationElement.ofClass("value", ClassDesc.of(classLiteralValue)))));
        });
    }

    private static String className(byte[] bytecode) {
        return ClassFile.of().parse(bytecode).thisClass().asInternalName().replace('/', '.');
    }
}
