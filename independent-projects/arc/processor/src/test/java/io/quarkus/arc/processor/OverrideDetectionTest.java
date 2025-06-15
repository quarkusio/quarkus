package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Injection.isOverriden;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.processor.Injection.MethodOverrideKey;
import io.quarkus.arc.processor.types.Bottom;
import io.quarkus.arc.processor.types.Top;
import io.quarkus.arc.processor.types.extrapkg.Middle;
import io.quarkus.arc.processor.types.extrapkg.Middle2;

public class OverrideDetectionTest {
    private Index index;

    @BeforeEach
    public void setUp() throws IOException {
        index = Index.of(SomeClass.class, SuperClass.class, SuperSuperClass.class, TheRoot.class,
                Top.class, Middle.class, Middle2.class, Bottom.class);
    }

    private Set<MethodOverrideKey> allMethodsOf(Class<?>... classes) {
        return Arrays.stream(classes)
                .map(index::getClassByName)
                .map(ClassInfo::methods)
                .flatMap(List::stream)
                .map(MethodOverrideKey::new)
                .collect(Collectors.toSet());
    }

    @Test
    public void shouldFindDirectOverriding() {
        Set<MethodOverrideKey> methods = allMethodsOf(SomeClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperClass.class).firstMethod("fromSuperClass");

        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isTrue();
    }

    @Test
    public void shouldFindGenericOverriding() {
        Set<MethodOverrideKey> methods = allMethodsOf(SomeClass.class, SuperClass.class);

        MethodInfo genericMethod = index.getClassByName(SuperSuperClass.class).firstMethod("generic");

        assertThat(isOverriden(new MethodOverrideKey(genericMethod), methods)).isTrue();
    }

    @Test
    public void shouldNotFindNonOverridenFromSuperClass() {
        Set<MethodOverrideKey> methods = allMethodsOf(SomeClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperClass.class).firstMethod("notOverridenFromSuperClass");

        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();
    }

    @Test
    public void shouldNotFindNonGenericNonOverridenFromSuperSuperClass() {
        Set<MethodOverrideKey> methods = allMethodsOf(SomeClass.class, SuperClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperSuperClass.class).firstMethod("notOverridenNonGeneric");

        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();
    }

    @Test
    public void shouldNotFindGenericNonOverridenFromSuperSuperClass() {
        Set<MethodOverrideKey> methods = allMethodsOf(SomeClass.class, SuperClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperSuperClass.class).firstMethod("notOverridenGeneric");

        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();
    }

    @Test
    public void shouldNotFindAlmostMatchingGeneric() {
        Set<MethodOverrideKey> methods = allMethodsOf(SomeClass.class, SuperClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperSuperClass.class).firstMethod("almostMatchingGeneric");

        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();
    }

    @Test
    public void shouldFindOverridenInTheMiddleOfHierarchy() {
        Set<MethodOverrideKey> methods = allMethodsOf(SomeClass.class, SuperClass.class, SuperSuperClass.class);

        MethodInfo parentMethod = index.getClassByName(TheRoot.class).firstMethod("generic");

        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isTrue();
    }

    @Test
    public void visibilityMiddleBottom() {
        Set<MethodOverrideKey> methods = allMethodsOf(Bottom.class);

        ClassInfo parent = index.getClassByName(Middle.class);

        MethodInfo parentMethod = parent.firstMethod("packagePrivateMethod");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();

        parentMethod = parent.firstMethod("privateMethod");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();

        parentMethod = parent.firstMethod("packagePrivateMethodToBecomeProtected");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();

        parentMethod = parent.firstMethod("packagePrivateMethodToBecomePublic");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();
    }

    @Test
    public void visibilityTopMiddleBottom() {
        Set<MethodOverrideKey> methods = allMethodsOf(Middle.class, Bottom.class);

        ClassInfo parent = index.getClassByName(Top.class);

        MethodInfo parentMethod = parent.firstMethod("publicMethod");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isTrue();

        parentMethod = parent.firstMethod("protectedMethod");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isTrue();

        parentMethod = parent.firstMethod("packagePrivateMethod");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isTrue();

        parentMethod = parent.firstMethod("privateMethod");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();

        parentMethod = parent.firstMethod("protectedMethodToBecomePublic");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isTrue();

        parentMethod = parent.firstMethod("packagePrivateMethodToBecomeProtected");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isTrue();

        parentMethod = parent.firstMethod("packagePrivateMethodToBecomePublic");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isTrue();
    }

    @Test
    public void visibilityTopMiddle2() {
        Set<MethodOverrideKey> methods = allMethodsOf(Middle2.class);

        ClassInfo parent = index.getClassByName(Top.class);

        MethodInfo parentMethod = parent.firstMethod("publicMethod");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isTrue();

        parentMethod = parent.firstMethod("protectedMethod");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isTrue();

        parentMethod = parent.firstMethod("packagePrivateMethod");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();

        parentMethod = parent.firstMethod("privateMethod");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();

        parentMethod = parent.firstMethod("protectedMethodToBecomePublic");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isTrue();

        parentMethod = parent.firstMethod("packagePrivateMethodToBecomeProtected");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();

        parentMethod = parent.firstMethod("packagePrivateMethodToBecomePublic");
        assertThat(isOverriden(new MethodOverrideKey(parentMethod), methods)).isFalse();
    }

    public static class SomeClass extends SuperClass<Boolean> {
        @Override
        void generic(Integer param) {
        }

        @Override
        void nonGeneric(String param) {
        }

        @Override
        void fromSuperClass(int param) {
        }
    }

    public static class SuperClass<V> extends SuperSuperClass<Integer, V> {
        void fromSuperClass(int param) {
        }

        void notOverridenFromSuperClass(int param) {
        }

        void almostMatchingGeneric(V param) {
        }
    }

    public static class SuperSuperClass<V, U> extends TheRoot<String, U, V> {
        @Override
        @Override
        void generic(V arg) {
        }

        void almostMatchingGeneric(Integer arg) {
        }

        void nonGeneric(String param) {
        }

        void notOverridenGeneric(V arg) {
        }

        void notOverridenNonGeneric(String param) {
        }
    }

    public static class TheRoot<U, V, X> {
        void generic(X param) {
        }
    }
}
