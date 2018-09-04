package org.jboss.protean.arc.processor;

import static org.jboss.protean.arc.processor.Basics.index;
import static org.jboss.protean.arc.processor.Basics.name;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.protean.arc.processor.BeanDeployment;
import org.jboss.protean.arc.processor.BeanInfo;
import org.jboss.protean.arc.processor.Beans;
import org.jboss.protean.arc.processor.types.Bar;
import org.jboss.protean.arc.processor.types.Foo;
import org.jboss.protean.arc.processor.types.FooQualifier;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.Test;

/**
 *
 * @author Martin Kouba
 */
public class BeanInfoQualifiersTest {

    @Test
    public void testQualifiers() throws IOException {
        Index index = index(Foo.class, Bar.class, FooQualifier.class);
        DotName fooName = name(Foo.class);
        DotName fooQualifierName = name(FooQualifier.class);
        ClassInfo fooClass = index.getClassByName(fooName);

        BeanInfo bean = Beans.createClassBean(fooClass, new BeanDeployment(index, null));

        AnnotationInstance requiredFooQualifier = index.getAnnotations(fooQualifierName).stream()
                .filter(a -> Kind.FIELD.equals(a.target().kind()) && a.target().asField().name().equals("foo")).findFirst().orElse(null);

        assertNotNull(requiredFooQualifier);
        // FooQualifier#alpha() is @Nonbinding
        assertTrue(Beans.hasQualifier(bean, requiredFooQualifier));
    }

}
