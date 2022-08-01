package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Basics.index;
import static io.quarkus.arc.processor.Basics.name;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.processor.types.Bar;
import io.quarkus.arc.processor.types.Foo;
import io.quarkus.arc.processor.types.FooQualifier;
import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Martin Kouba
 */
public class BeanInfoQualifiersTest {

    @Test
    public void testQualifiers() throws IOException {
        Index index = index(Foo.class, Bar.class, FooQualifier.class, AbstractList.class, AbstractCollection.class,
                List.class, Collection.class, Object.class, String.class, Iterable.class);
        DotName fooName = name(Foo.class);
        DotName fooQualifierName = name(FooQualifier.class);
        ClassInfo fooClass = index.getClassByName(fooName);

        BeanInfo bean = Beans.createClassBean(fooClass,
                BeanProcessor.builder().setBeanArchiveIndex(index).build().getBeanDeployment(),
                null);

        AnnotationInstance requiredFooQualifier = index.getAnnotations(fooQualifierName).stream()
                .filter(a -> Kind.FIELD.equals(a.target().kind()) && a.target().asField().name().equals("foo")).findFirst()
                .orElse(null);

        assertNotNull(requiredFooQualifier);
        // FooQualifier#alpha() is @Nonbinding
        assertTrue(Beans.hasQualifier(bean, requiredFooQualifier));
    }

}
