package io.quarkus.arc.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.Index;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.processor.types.Bar;
import io.quarkus.arc.processor.types.Foo;
import io.quarkus.arc.processor.types.FooQualifier;

/**
 *
 * @author Martin Kouba
 */
public class BeanInfoTypesTest {

    @Test
    public void testResolver() throws IOException {

        Index index = Index.of(Foo.class, Bar.class, FooQualifier.class, AbstractList.class, AbstractCollection.class,
                Collection.class, List.class, Iterable.class, Object.class, String.class);

        BeanDeployment deployment = BeanProcessor.builder().setImmutableBeanArchiveIndex(index).build().getBeanDeployment();

        ClassInfo fooClass = index.getClassByName(Foo.class);
        BeanInfo fooBean = Beans.createClassBean(fooClass, deployment, null);
        Set<Type> types = fooBean.getTypes();
        // Foo, AbstractList<String>, AbstractCollection<String>, List<String>, Collection<String>, Iterable<String>, Object
        assertEquals(7, types.size());
        assertTrue(types.contains(ClassType.create(Foo.class)));
        assertTrue(types.contains(ParameterizedType.builder(AbstractList.class)
                .addArgument(ClassType.create(String.class))
                .build()));
        assertTrue(types.contains(ParameterizedType.builder(List.class)
                .addArgument(ClassType.create(String.class))
                .build()));
        assertTrue(types.contains(ParameterizedType.builder(Collection.class)
                .addArgument(ClassType.create(String.class))
                .build()));
        assertTrue(types.contains(ParameterizedType.builder(AbstractCollection.class)
                .addArgument(ClassType.create(String.class))
                .build()));
        assertTrue(types.contains(ParameterizedType.builder(Iterable.class)
                .addArgument(ClassType.create(String.class))
                .build()));

    }

}
