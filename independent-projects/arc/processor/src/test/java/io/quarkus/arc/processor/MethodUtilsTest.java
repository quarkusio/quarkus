package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Basics.index;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 17/07/2019
 */
public class MethodUtilsTest {

    private Index index;

    @BeforeEach
    public void setUp() throws IOException {
        index = index(SomeClass.class, SuperClass.class, SuperSuperClass.class, TheRoot.class);
    }

    private Set<MethodInfo> gatherMethodsFromClasses(Class<?>... classes) {
        return Arrays.stream(classes)
                .map(this::classInfo)
                .map(ClassInfo::methods)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    @Test
    public void shouldFindDirectOverriding() {
        ClassInfo aClass = classInfo(SomeClass.class.getName());
        ClassInfo superClass = classInfo(SuperClass.class.getName());

        MethodInfo parentMethod = superClass.firstMethod("fromSuperClass");
        assertThat(Methods.isOverriden(parentMethod, aClass.methods())).isTrue();
    }

    @Test
    public void shouldFindGenericOverriding() {
        Collection<MethodInfo> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class);

        ClassInfo grandma = classInfo(SuperSuperClass.class);

        MethodInfo genericMethod = grandma.firstMethod("generic");

        assertThat(Methods.isOverriden(genericMethod, methods)).isTrue();
    }

    @Test
    public void shouldNotFindNonOverridenFromSuperClass() {
        ClassInfo aClass = classInfo(SomeClass.class.getName());
        ClassInfo superClass = classInfo(SuperClass.class.getName());

        MethodInfo parentMethod = superClass.firstMethod("notOverridenFromSuperClass");
        assertThat(Methods.isOverriden(parentMethod, aClass.methods())).isFalse();
    }

    @Test
    public void shouldNotFindNonGenericNonOverridenFromSuperSuperClass() {
        Collection<MethodInfo> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class);

        ClassInfo grandma = classInfo(SuperSuperClass.class.getName());

        MethodInfo parentMethod = grandma.firstMethod("notOverridenNonGeneric");
        assertThat(Methods.isOverriden(parentMethod, methods)).isFalse();
    }

    @Test
    public void shouldNotFindGenericNonOverridenFromSuperSuperClass() {
        Collection<MethodInfo> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class);

        ClassInfo grandma = classInfo(SuperSuperClass.class.getName());

        MethodInfo parentMethod = grandma.firstMethod("notOverridenGeneric");
        assertThat(Methods.isOverriden(parentMethod, methods)).isFalse();
    }

    @Test
    public void shouldNotFindAlmostMatchingGeneric() {
        Collection<MethodInfo> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class);

        ClassInfo grandma = classInfo(SuperSuperClass.class.getName());

        MethodInfo parentMethod = grandma.firstMethod("almostMatchingGeneric");
        assertThat(Methods.isOverriden(parentMethod, methods)).isFalse();
    }

    @Test
    public void shouldFindOverridenInTheMiddleOfHierarchy() {
        Collection<MethodInfo> methods = gatherMethodsFromClasses(SomeClass.class, SuperClass.class, SuperSuperClass.class);

        ClassInfo root = classInfo(TheRoot.class.getName());

        MethodInfo parentMethod = root.firstMethod("generic");
        assertThat(Methods.isOverriden(parentMethod, methods)).isTrue();
    }

    private ClassInfo classInfo(Class<?> aClass) {
        return index.getClassByName(DotName.createSimple(aClass.getName()));
    }

    private ClassInfo classInfo(String name) {
        return index.getClassByName(DotName.createSimple(name));
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
