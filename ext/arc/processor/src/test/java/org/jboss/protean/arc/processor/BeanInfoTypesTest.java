package org.jboss.protean.arc.processor;

import static org.jboss.protean.arc.processor.Basics.index;
import static org.jboss.protean.arc.processor.Basics.name;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.jboss.protean.arc.processor.BeanDeployment;
import org.jboss.protean.arc.processor.BeanInfo;
import org.jboss.protean.arc.processor.Beans;
import org.jboss.protean.arc.processor.types.Bar;
import org.jboss.protean.arc.processor.types.Foo;
import org.jboss.protean.arc.processor.types.FooQualifier;
import org.junit.Test;

/**
 *
 * @author Martin Kouba
 */
public class BeanInfoTypesTest {

    @Test
    public void testResolver() throws IOException {

        Index index = index(Foo.class, Bar.class, FooQualifier.class, AbstractList.class, AbstractCollection.class, Collection.class, List.class,
                Iterable.class);

        BeanDeployment deployment = new BeanDeployment(index, null);
        DotName fooName = name(Foo.class);

        ClassInfo fooClass = index.getClassByName(fooName);
        BeanInfo fooBean = Beans.createClassBean(fooClass, deployment);
        Set<Type> types = fooBean.getTypes();
        // System.out.println(types);
        // Foo, AbstractList<String>, AbstractCollection<String>, List<String>, Collection<String>, Iterable<String>
        assertEquals(6, types.size());
        assertTrue(types.contains(Type.create(fooName, Kind.CLASS)));
        assertTrue(types.contains(ParameterizedType.create(name(AbstractList.class), new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));
        assertTrue(types.contains(ParameterizedType.create(name(List.class), new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));
        assertTrue(types.contains(ParameterizedType.create(name(Collection.class), new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));
        assertTrue(types.contains(ParameterizedType.create(name(AbstractCollection.class), new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));
        assertTrue(types.contains(ParameterizedType.create(name(Iterable.class), new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));

    }

}
