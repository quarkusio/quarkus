package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.constant.ClassDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.paths.PathTree;
import io.smallrye.classfile.Annotation;
import io.smallrye.classfile.AnnotationElement;
import io.smallrye.classfile.AnnotationValue;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.attribute.RuntimeVisibleAnnotationsAttribute;

class SubstitutionRootCollectorTest {

    private static final ClassDesc TARGET_CLASS = ClassDesc.of("com.oracle.svm.core.annotate.TargetClass");

    @TempDir
    Path dir;

    @Test
    void classLiteralTarget() {
        writeClass("lib.Target_Subst", Annotation.of(TARGET_CLASS,
                AnnotationElement.ofClass("value", ClassDesc.of("lib.Target"))));

        Collected collected = collect();

        assertThat(collected.roots).isEmpty();
        assertThat(collected.conditional).containsExactlyInAnyOrder(
                Map.entry("lib.Target", "lib.Target_Subst"), Map.entry("lib.Target_Subst", "lib.Target"));
    }

    @Test
    void classNameTarget() {
        writeClass("lib.Target_Subst", Annotation.of(TARGET_CLASS,
                AnnotationElement.ofString("className", "lib.Target")));

        Collected collected = collect();

        assertThat(collected.declaredClasses).isEmpty();
        assertThat(collected.roots).isEmpty();
        assertThat(collected.conditional).containsExactlyInAnyOrder(
                Map.entry("lib.Target", "lib.Target_Subst"), Map.entry("lib.Target_Subst", "lib.Target"));
    }

    @Test
    void innerClassTarget() {
        writeClass("lib.Target_Outer_Inner", Annotation.of(TARGET_CLASS,
                AnnotationElement.ofString("className", "lib.Outer"),
                AnnotationElement.of("innerClass", AnnotationValue.ofArray(
                        AnnotationValue.ofString("Inner"), AnnotationValue.ofString("Deeper")))));

        Collected collected = collect();

        assertThat(collected.roots).isEmpty();
        assertThat(collected.conditional).containsExactlyInAnyOrder(
                Map.entry("lib.Outer$Inner$Deeper", "lib.Target_Outer_Inner"),
                Map.entry("lib.Target_Outer_Inner", "lib.Outer$Inner$Deeper"));
        // native-image looks up Inner in Outer's and Deeper in Outer$Inner's declared classes
        assertThat(collected.declaredClasses).containsExactlyInAnyOrder(
                Map.entry("lib.Target_Outer_Inner", "lib.Outer"),
                Map.entry("lib.Target_Outer_Inner", "lib.Outer$Inner"));
    }

    @Test
    void classLiteralWithInnerClassTarget() {
        writeClass("lib.Target_Outer_Inner", Annotation.of(TARGET_CLASS,
                AnnotationElement.ofClass("value", ClassDesc.of("lib.Outer")),
                AnnotationElement.of("innerClass", AnnotationValue.ofArray(AnnotationValue.ofString("Inner")))));

        Collected collected = collect();

        assertThat(collected.roots).isEmpty();
        assertThat(collected.conditional).containsExactlyInAnyOrder(
                Map.entry("lib.Outer$Inner", "lib.Target_Outer_Inner"),
                Map.entry("lib.Target_Outer_Inner", "lib.Outer$Inner"));
        assertThat(collected.declaredClasses).containsExactly(Map.entry("lib.Target_Outer_Inner", "lib.Outer"));
    }

    @Test
    void explicitNoClassNameProviderIsIgnored() {
        writeClass("lib.Target_Subst", Annotation.of(TARGET_CLASS,
                AnnotationElement.ofString("className", "lib.Target"),
                AnnotationElement.ofClass("classNameProvider",
                        ClassDesc.of("com.oracle.svm.core.annotate.TargetClass$NoClassNameProvider"))));

        Collected collected = collect();

        assertThat(collected.roots).isEmpty();
        assertThat(collected.conditional).containsExactlyInAnyOrder(
                Map.entry("lib.Target", "lib.Target_Subst"), Map.entry("lib.Target_Subst", "lib.Target"));
    }

    @Test
    void unexpectedAnnotationValueKindIsUnconditional() {
        writeClass("lib.Target_Weird", Annotation.of(TARGET_CLASS,
                AnnotationElement.ofString("value", "lib.Target")));

        Collected collected = collect();

        assertThat(collected.roots).containsExactly("lib.Target_Weird");
        assertThat(collected.conditional).isEmpty();
    }

    @Test
    void classNameProviderIsUnconditional() {
        writeClass("lib.Target_Provided", Annotation.of(TARGET_CLASS,
                AnnotationElement.ofClass("classNameProvider", ClassDesc.of("lib.NameProvider"))));

        Collected collected = collect();

        assertThat(collected.roots).containsExactly("lib.Target_Provided");
        assertThat(collected.conditional).isEmpty();
    }

    @Test
    void markedClassWithoutTargetClassIsUnconditional() {
        writeClass("lib.AutoFeature", Annotation.of(ClassDesc.of("com.oracle.svm.core.annotate.AutomaticFeature")));

        Collected collected = collect();

        assertThat(collected.roots).containsExactly("lib.AutoFeature");
        assertThat(collected.conditional).isEmpty();
    }

    @Test
    void malformedMarkedClassIsUnconditional() throws IOException {
        Path file = dir.resolve("lib/Broken.class");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "not a class file com/oracle/svm/core/annotate/TargetClass");

        Collected collected = collect();

        assertThat(collected.roots).containsExactly("lib.Broken");
        assertThat(collected.conditional).isEmpty();
    }

    @Test
    void unmarkedClassIsIgnored() {
        writeClass("lib.Plain", Annotation.of(ClassDesc.of("lib.SomeAnnotation")));

        Collected collected = collect();

        assertThat(collected.roots).isEmpty();
        assertThat(collected.conditional).isEmpty();
    }

    private Collected collect() {
        Collected collected = new Collected();
        SubstitutionRootCollector.collect(PathTree.ofDirectoryOrArchive(dir),
                item -> collected.roots.add(item.getClassName()),
                item -> collected.conditional.add(Map.entry(item.getConditionClassName(), item.getClassName())),
                item -> collected.declaredClasses.add(Map.entry(item.getConditionClassName(), item.getHolderClassName())));
        return collected;
    }

    private void writeClass(String name, Annotation annotation) {
        byte[] bytecode = ClassFile.of().build(ClassDesc.of(name), classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_PUBLIC);
            classBuilder.with(RuntimeVisibleAnnotationsAttribute.of(annotation));
        });
        Path file = dir.resolve(name.replace('.', '/') + ".class");
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, bytecode);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final class Collected {
        final Set<String> roots = new HashSet<>();
        final List<Map.Entry<String, String>> conditional = new ArrayList<>();
        final List<Map.Entry<String, String>> declaredClasses = new ArrayList<>();
    }
}
