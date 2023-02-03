package io.quarkus.arc.processor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.junit.jupiter.api.Test;

public class TypesTest {

    @Test
    public void testGetTypeClosure() throws IOException {
        IndexView index = Basics.index(Foo.class, Baz.class, Producer.class, Object.class, List.class, Collection.class,
                Iterable.class, Set.class, Eagle.class, Bird.class, Animal.class, AnimalHolder.class, MyRawBean.class,
                MyBean.class, MyInterface.class, MySuperInterface.class);
        DotName bazName = DotName.createSimple(Baz.class.getName());
        DotName fooName = DotName.createSimple(Foo.class.getName());
        DotName producerName = DotName.createSimple(Producer.class.getName());
        ClassInfo fooClass = index.getClassByName(fooName);
        Map<ClassInfo, Map<String, Type>> resolvedTypeVariables = new HashMap<>();
        BeanDeployment dummyDeployment = BeanProcessor.builder().setImmutableBeanArchiveIndex(index).build()
                .getBeanDeployment();

        // Baz, Foo<String>, Object
        Set<Type> bazTypes = Types.getTypeClosure(index.getClassByName(bazName), null,
                Collections.emptyMap(),
                dummyDeployment,
                resolvedTypeVariables::put, new HashSet<>());
        assertEquals(3, bazTypes.size());
        assertTrue(bazTypes.contains(Type.create(bazName, Kind.CLASS)));
        assertTrue(bazTypes.contains(ParameterizedType.create(fooName,
                new Type[] { Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS) },
                null)));
        assertEquals(resolvedTypeVariables.size(), 1);
        assertTrue(resolvedTypeVariables.containsKey(fooClass));
        assertEquals(resolvedTypeVariables.get(fooClass).get(fooClass.typeParameters().get(0).identifier()),
                Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS));

        resolvedTypeVariables.clear();
        // Foo<T>, Object
        Set<Type> fooTypes = Types.getClassBeanTypeClosure(fooClass,
                dummyDeployment);
        assertEquals(2, fooTypes.size());
        for (Type t : fooTypes) {
            if (t.kind().equals(Kind.PARAMETERIZED_TYPE)) {
                ParameterizedType fooType = t.asParameterizedType();
                assertEquals("T", fooType.arguments().get(0).asTypeVariable().identifier());
                assertEquals(DotNames.OBJECT, fooType.arguments().get(0).asTypeVariable().bounds().get(0).name());
                assertTrue(Types.containsTypeVariable(fooType));
            }
        }
        ClassInfo producerClass = index.getClassByName(producerName);
        for (String name : Arrays.asList("produce", "produceNested", "produceTypeVar",
                "produceArray", "produceNestedArray", "produceTypeVarArray")) {
            assertThrows(DefinitionException.class,
                    () -> Types.getProducerMethodTypeClosure(producerClass.method(name), dummyDeployment));
            assertThrows(DefinitionException.class,
                    () -> Types.getProducerFieldTypeClosure(producerClass.field(name), dummyDeployment));
        }

        // now assert following ones do NOT throw, the wildcard in the hierarchy is just ignored
        final String wildcardInTypeHierarchy = "eagleProducer";
        assertDoesNotThrow(
                () -> verifyEagleTypes(
                        Types.getProducerMethodTypeClosure(producerClass.method(wildcardInTypeHierarchy), dummyDeployment)));
        assertDoesNotThrow(
                () -> verifyEagleTypes(
                        Types.getProducerFieldTypeClosure(producerClass.field(wildcardInTypeHierarchy), dummyDeployment)));
        // now do the same for a non-producer scenario with Eagle class
        assertDoesNotThrow(
                () -> verifyEagleTypes(Types.getClassBeanTypeClosure(index.getClassByName(DotName.createSimple(Eagle.class)),
                        dummyDeployment)));

        // raw type bean
        Set<Type> rawBeanTypes = Types.getClassBeanTypeClosure(index.getClassByName(DotName.createSimple(MyRawBean.class)),
                dummyDeployment);
        assertEquals(rawBeanTypes.size(), 5);
        assertContainsType(MyRawBean.class, rawBeanTypes);
        assertContainsType(MyBean.class, rawBeanTypes);
        assertContainsType(MyInterface.class, rawBeanTypes);
        // according to JLS, for raw type generics, their superclasses have erasure applied so this should be a match
        assertContainsType(MySuperInterface.class, rawBeanTypes);
        assertContainsType(Object.class, rawBeanTypes);
    }

    private void verifyEagleTypes(Set<Type> types) {
        for (Type type : types) {
            if (type.kind().equals(Kind.PARAMETERIZED_TYPE)) {
                assertNotEquals(DotName.createSimple(AnimalHolder.class), type.name());
            }
        }
        assertEquals(3, types.size());
    }

    private void assertContainsType(Class<?> clazz, Set<Type> rawBeanTypes) {
        assertTrue(rawBeanTypes.contains(Type.create(DotName.createSimple(clazz.getName()), Kind.CLASS)));
    }

    static class Foo<T> {

        T field;

    }

    static class Baz extends Foo<String> {

    }

    static class Producer<T> {

        public List<? extends Number> produce() {
            return null;
        }

        public List<Set<? extends Number>> produceNested() {
            return null;
        }

        public T produceTypeVar() {
            return null;
        }

        public List<? extends Number>[][] produceArray() {
            return null;
        }

        public List<Set<? extends Number>>[][] produceNestedArray() {
            return null;
        }

        public T[][] produceTypeVarArray() {
            return null;
        }

        List<? extends Number> produce;

        List<Set<? extends Number>> produceNested;

        public T produceTypeVar;

        List<? extends Number>[] produceArray;

        List<Set<? extends Number>>[] produceNestedArray;

        public T[] produceTypeVarArray;

        // following producer should NOT throw an exception because the return types doesn't contain wildcard
        // but the hierarchy of the return type actually contains one
        // taken from CDI TCK setup, see https://github.com/jakartaee/cdi-tck/blob/4.0.7/impl/src/main/java/org/jboss/cdi/tck/tests/definition/bean/types/illegal/BeanTypesWithIllegalTypeTest.java
        public Eagle<T> eagleProducer() {
            return new Eagle<>();
        }

        public Eagle<T> eagleProducer;
    }

    static class Eagle<T> extends Bird<T> {
    }

    static class Bird<T> extends AnimalHolder<Animal<? extends T>> {
    }

    static class Animal<T> {
    }

    static class AnimalHolder<T extends Animal> {
    }

    static class MyRawBean extends MyBean {
    }

    static class MyBean<T> implements MyInterface {
    }

    interface MyInterface extends MySuperInterface<Number> {
    }

    interface MySuperInterface<T> {
    }
}
