package io.quarkus.deployment.pkg.steps;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.deployment.pkg.TreeShakeConfig;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.smallrye.classfile.Annotation;
import io.smallrye.classfile.AnnotationElement;
import io.smallrye.classfile.ClassBuilder;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.attribute.InnerClassInfo;
import io.smallrye.classfile.attribute.InnerClassesAttribute;
import io.smallrye.classfile.attribute.RuntimeVisibleAnnotationsAttribute;

/**
 * Builds {@link JarTreeShakeInput} instances from in-memory bytecode for tree-shaker tests.
 */
final class TreeShakeInputFixture {

    private final JarTreeShakeInput input = new JarTreeShakeInput(TreeShakeConfig.TreeShakeMode.CLASSES,
            Runtime.version().feature());

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
            input.depBytecode.put(name, () -> bytecode);
            input.classToDep.put(name, key);
        }
        return this;
    }

    /**
     * Adds application classes, each of which becomes a root.
     */
    TreeShakeInputFixture application(byte[]... classes) {
        for (byte[] bytecode : classes) {
            final String name = className(bytecode);
            input.appBytecode.put(name, () -> bytecode);
            input.roots.add(name);
        }
        return this;
    }

    /**
     * Adds the bytecode of a class that isn't on the classpath and becomes a root, like a predefined class.
     */
    TreeShakeInputFixture rootBytecode(byte[] bytecode) {
        return application(bytecode);
    }

    /**
     * Adds multi-release bytecode of a dependency class for a Java version newer than the analyzed one.
     */
    TreeShakeInputFixture higherVersion(byte[] bytecode) {
        input.higherVersionBytecode.computeIfAbsent(className(bytecode), k -> new ArrayList<>()).add(() -> bytecode);
        return this;
    }

    TreeShakeInputFixture root(String className) {
        input.roots.add(className);
        return this;
    }

    TreeShakeInputFixture conditionalRoot(String condition, String className) {
        input.conditionalRoots.computeIfAbsent(condition, k -> new HashSet<>()).add(className);
        return this;
    }

    /**
     * Requires the declared (member) classes of {@code holderClassName} to be loadable if {@code condition} is reachable.
     */
    TreeShakeInputFixture declaredClasses(String condition, String holderClassName) {
        input.conditionalDeclaredClasses.computeIfAbsent(condition, k -> new HashSet<>()).add(holderClassName);
        return this;
    }

    /**
     * Excludes the dependency identified by the given artifact key from tree shaking.
     */
    TreeShakeInputFixture excludedArtifact(String artifactKey) {
        input.excludedArtifacts.add(ArtifactKey.fromString(artifactKey));
        return this;
    }

    JarTreeShakeInput build() {
        input.allKnownClasses.addAll(input.depBytecode.keySet());
        return input;
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
            addReferencingMethod(classBuilder, referencedClasses);
        });
    }

    private static void addReferencingMethod(ClassBuilder classBuilder, String... referencedClasses) {
        classBuilder.withMethodBody("refs", MethodTypeDesc.of(ConstantDescs.CD_void),
                ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC, code -> {
                    for (String referenced : referencedClasses) {
                        code.loadConstant(ClassDesc.of(referenced));
                        code.pop();
                    }
                    code.return_();
                });
    }

    /**
     * Generates a class with the given superclass whose static method loads a class literal of each of the
     * referenced classes.
     */
    static byte[] classExtending(String name, String superName, String... referencedClasses) {
        return ClassFile.of().build(ClassDesc.of(name), classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_PUBLIC);
            classBuilder.withSuperclass(ClassDesc.of(superName));
            addReferencingMethod(classBuilder, referencedClasses);
        });
    }

    /**
     * Generates a class implementing the given interface.
     */
    static byte[] classImplementing(String name, String interfaceName) {
        return ClassFile.of().build(ClassDesc.of(name), classBuilder -> classBuilder.withFlags(ClassFile.ACC_PUBLIC)
                .withInterfaceSymbols(ClassDesc.of(interfaceName)));
    }

    /**
     * Generates a class declaring the given member classes in its {@code InnerClasses} attribute.
     */
    static byte[] classWithMembers(String name, String... memberClasses) {
        return ClassFile.of().build(ClassDesc.of(name), classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_PUBLIC);
            List<InnerClassInfo> members = new ArrayList<>(memberClasses.length);
            for (String member : memberClasses) {
                members.add(InnerClassInfo.of(ClassDesc.of(member), Optional.of(ClassDesc.of(name)),
                        Optional.of(member.substring(member.lastIndexOf('$') + 1)),
                        ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC));
            }
            classBuilder.with(InnerClassesAttribute.of(members));
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
