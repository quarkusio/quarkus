package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Basics.index;
import static io.quarkus.arc.processor.Basics.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.processor.types.Bar;
import io.quarkus.arc.processor.types.Foo;
import io.quarkus.arc.processor.types.FooQualifier;
import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Martin Kouba
 */
public class BeanInfoTypesTest {

    @Test
    public void testResolver() throws IOException {

        Index index = index(Foo.class, Bar.class, FooQualifier.class, AbstractList.class, AbstractCollection.class,
                Collection.class, List.class,
                Iterable.class, Object.class, String.class);

        BeanDeployment deployment = BeanProcessor.builder().setBeanArchiveIndex(index).build().getBeanDeployment();
        DotName fooName = name(Foo.class);

        ClassInfo fooClass = index.getClassByName(fooName);
        BeanInfo fooBean = Beans.createClassBean(fooClass, deployment, null);
        Set<Type> types = fooBean.getTypes();
        // Foo, AbstractList<String>, AbstractCollection<String>, List<String>, Collection<String>, Iterable<String>, Object
        assertEquals(7, types.size());
        assertTrue(types.contains(Type.create(fooName, Kind.CLASS)));
        assertTrue(types.contains(ParameterizedType.create(name(AbstractList.class),
                new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));
        assertTrue(types.contains(
                ParameterizedType.create(name(List.class), new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));
        assertTrue(types.contains(ParameterizedType.create(name(Collection.class),
                new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));
        assertTrue(types.contains(ParameterizedType.create(name(AbstractCollection.class),
                new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));
        assertTrue(types.contains(ParameterizedType.create(name(Iterable.class),
                new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));

    }

}
