package io.quarkus.arc.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;
import org.junit.jupiter.api.Test;

public class TypesTest {

    @Test
    public void testGetTypeClosure() throws IOException {
        IndexView index = Basics.index(Foo.class, Baz.class, Producer.class, Object.class, List.class, Collection.class,
                Iterable.class);
        DotName bazName = DotName.createSimple(Baz.class.getName());
        DotName fooName = DotName.createSimple(Foo.class.getName());
        DotName producerName = DotName.createSimple(Producer.class.getName());
        ClassInfo fooClass = index.getClassByName(fooName);
        Map<ClassInfo, Map<TypeVariable, Type>> resolvedTypeVariables = new HashMap<>();
        BeanDeployment dummyDeployment = BeanProcessor.builder().setBeanArchiveIndex(index).build().getBeanDeployment();

        // Baz, Foo<String>, Object
        Set<Type> bazTypes = Types.getTypeClosure(index.getClassByName(bazName), null,
                Collections.emptyMap(),
                dummyDeployment,
                resolvedTypeVariables::put);
        assertEquals(3, bazTypes.size());
        assertTrue(bazTypes.contains(Type.create(bazName, Kind.CLASS)));
        assertTrue(bazTypes.contains(ParameterizedType.create(fooName,
                new Type[] { Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS) },
                null)));
        assertEquals(resolvedTypeVariables.size(), 1);
        assertTrue(resolvedTypeVariables.containsKey(fooClass));
        assertEquals(resolvedTypeVariables.get(fooClass).get(fooClass.typeParameters().get(0)),
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
        String producersName = "produce";
        MethodInfo producerMethod = producerClass.method(producersName);
        // Object is the sole type
        Set<Type> producerMethodTypes = Types.getProducerMethodTypeClosure(producerMethod,
                dummyDeployment);
        assertEquals(1, producerMethodTypes.size());

        // Object is the sole type
        FieldInfo producerField = producerClass.field(producersName);
        Set<Type> producerFieldTypes = Types.getProducerFieldTypeClosure(producerField,
                dummyDeployment);
        assertEquals(1, producerFieldTypes.size());
    }

    static class Foo<T> {

        T field;

    }

    static class Baz extends Foo<String> {

    }

    static class Producer {

        public List<? extends Number> produce() {
            return null;
        }

        List<? extends Number> produce;
    }
}
