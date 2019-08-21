package io.quarkus.arc.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;
import org.junit.Test;

public class TypesTest {

    @Test
    public void testGetTypeClosure() throws IOException {
        IndexView index = Basics.index(Foo.class, Baz.class);
        DotName bazName = DotName.createSimple(Baz.class.getName());
        DotName fooName = DotName.createSimple(Foo.class.getName());
        ClassInfo fooClass = index.getClassByName(fooName);
        Map<ClassInfo, Map<TypeVariable, Type>> resolvedTypeVariables = new HashMap<>();
        Set<Type> types = Types.getTypeClosure(index.getClassByName(bazName), false,
                Collections.emptyMap(),
                new BeanDeployment(index, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                        Collections.emptyList(), null,
                        false, Collections.emptyList(), Collections.emptyMap(), Collections.emptyList()),
                resolvedTypeVariables::put);
        assertEquals(2, types.size());
        assertTrue(types.contains(Type.create(bazName, Kind.CLASS)));
        assertTrue(types.contains(ParameterizedType.create(fooName,
                new Type[] { Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS) },
                null)));
        assertEquals(resolvedTypeVariables.size(), 1);
        assertTrue(resolvedTypeVariables.containsKey(fooClass));
        assertEquals(resolvedTypeVariables.get(fooClass).get(fooClass.typeParameters().get(0)),
                Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS));
    }

    static class Foo<T> {

        T field;

    }

    static class Baz extends Foo<String> {

    }
}
