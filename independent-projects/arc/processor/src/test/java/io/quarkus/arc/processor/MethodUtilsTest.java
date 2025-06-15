package io.quarkus.arc.processor;

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

import io.quarkus.arc.processor.types.Bottom;
import io.quarkus.arc.processor.types.Top;
import io.quarkus.arc.processor.types.extrapkg.Middle;
import io.quarkus.arc.processor.types.extrapkg.Middle2;

public class MethodUtilsTest {

    private Index index;

    @BeforeEach
    public void setUp() throws IOException {
        index = Index.of(SomeClass.class, SuperClass.class, SuperSuperClass.class, TheRoot.class,
                Top.class, Middle.class, Middle2.class, Bottom.class);
    }

    private Set<Methods.MethodKey> gatherMethodsFromClasses(Class<?>... classes) {
        return Arrays.stream(classes)
                .map(index::getClassByName)
                .map(ClassInfo::methods)
                .flatMap(List::stream)
                .map(Methods.MethodKey::new)
                .collect(Collectors.toSet());
    }

    @Test
    public void shouldFindDirectOverriding() {
        Set<Methods.MethodKey> methods = gatherMethodsFromClasses(SomeClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperClass.class).firstMethod("fromSuperClass");

        assertThat(Methods.isOverriden(new Methods.MethodKey(parentMethod), methods)).isTrue();
    }

    @Test
    public void shouldFindGenericOverriding() {
        Set<Methods.MethodKey> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class);

        MethodInfo genericMethod = index.getClassByName(SuperSuperClass.class).firstMethod("generic");

        assertThat(Methods.isOverriden(new Methods.MethodKey(genericMethod), methods)).isTrue();
    }

    @Test
    public void shouldNotFindNonOverridenFromSuperClass() {
        Set<Methods.MethodKey> methods = gatherMethodsFromClasses(SomeClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperClass.class).firstMethod("notOverridenFromSuperClass");

        assertThat(Methods.isOverriden(new Methods.MethodKey(parentMethod), methods)).isFalse();
    }

    @Test
    public void shouldNotFindNonGenericNonOverridenFromSuperSuperClass() {
        Set<Methods.MethodKey> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperSuperClass.class).firstMethod("notOverridenNonGeneric");

        assertThat(Methods.isOverriden(new Methods.MethodKey(parentMethod), methods)).isFalse();
    }

    @Test
    public void shouldNotFindGenericNonOverridenFromSuperSuperClass() {
        Set<Methods.MethodKey> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperSuperClass.class).firstMethod("notOverridenGeneric");

        assertThat(Methods.isOverriden(new Methods.MethodKey(parentMethod), methods)).isFalse();
    }

    @Test
    public void shouldNotFindAlmostMatchingGeneric() {
        Set<Methods.MethodKey> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperSuperClass.class).firstMethod("almostMatchingGeneric");

        assertThat(Methods.isOverriden(new Methods.MethodKey(parentMethod), methods)).isFalse();
    }

    @Test
    public void shouldFindOverridenInTheMiddleOfHierarchy() {
        Set<Methods.MethodKey> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class, SuperSuperClass.class);

        MethodInfo parentMethod = index.getClassByName(TheRoot.class).firstMethod("generic");

        assertThat(Methods.isOverriden(new Methods.MethodKey(parentMethod), methods)).isTrue();
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
