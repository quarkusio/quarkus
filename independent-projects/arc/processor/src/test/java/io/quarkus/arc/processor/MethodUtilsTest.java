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
import org.jboss.jandex.MethodSignatureKey;
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

    private Set<MethodSignatureKey> gatherMethodsFromClasses(Class<?>... classes) {
        return Arrays.stream(classes)
                .map(index::getClassByName)
                .map(ClassInfo::methods)
                .flatMap(List::stream)
                .map(MethodInfo::signatureKey)
                .collect(Collectors.toSet());
    }

    @Test
    public void shouldFindDirectOverriding() {
        Set<MethodSignatureKey> methods = gatherMethodsFromClasses(SomeClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperClass.class).firstMethod("fromSuperClass");

        assertThat(Methods.isOverridden(parentMethod.signatureKey(), methods)).isTrue();
    }

    @Test
    public void shouldFindGenericOverriding() {
        Set<MethodSignatureKey> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class);

        MethodInfo genericMethod = index.getClassByName(SuperSuperClass.class).firstMethod("generic");

        assertThat(Methods.isOverridden(genericMethod.signatureKey(), methods)).isTrue();
    }

    @Test
    public void shouldNotFindNonOverridenFromSuperClass() {
        Set<MethodSignatureKey> methods = gatherMethodsFromClasses(SomeClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperClass.class).firstMethod("notOverridenFromSuperClass");

        assertThat(Methods.isOverridden(parentMethod.signatureKey(), methods)).isFalse();
    }

    @Test
    public void shouldNotFindNonGenericNonOverridenFromSuperSuperClass() {
        Set<MethodSignatureKey> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperSuperClass.class).firstMethod("notOverridenNonGeneric");

        assertThat(Methods.isOverridden(parentMethod.signatureKey(), methods)).isFalse();
    }

    @Test
    public void shouldNotFindGenericNonOverridenFromSuperSuperClass() {
        Set<MethodSignatureKey> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperSuperClass.class).firstMethod("notOverridenGeneric");

        assertThat(Methods.isOverridden(parentMethod.signatureKey(), methods)).isFalse();
    }

    @Test
    public void shouldNotFindAlmostMatchingGeneric() {
        Set<MethodSignatureKey> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class);

        MethodInfo parentMethod = index.getClassByName(SuperSuperClass.class).firstMethod("almostMatchingGeneric");

        assertThat(Methods.isOverridden(parentMethod.signatureKey(), methods)).isFalse();
    }

    @Test
    public void shouldFindOverridenInTheMiddleOfHierarchy() {
        Set<MethodSignatureKey> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class, SuperSuperClass.class);

        MethodInfo parentMethod = index.getClassByName(TheRoot.class).firstMethod("generic");

        assertThat(Methods.isOverridden(parentMethod.signatureKey(), methods)).isTrue();
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
