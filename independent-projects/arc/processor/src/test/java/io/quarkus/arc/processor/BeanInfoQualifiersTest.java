/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Basics.index;
import static io.quarkus.arc.processor.Basics.name;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.processor.types.Bar;
import io.quarkus.arc.processor.types.Foo;
import io.quarkus.arc.processor.types.FooQualifier;
import java.io.IOException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
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

        BeanInfo bean = Beans.createClassBean(fooClass, new BeanDeployment(index, null, null));

        AnnotationInstance requiredFooQualifier = index.getAnnotations(fooQualifierName).stream()
                .filter(a -> Kind.FIELD.equals(a.target().kind()) && a.target().asField().name().equals("foo")).findFirst()
                .orElse(null);

        assertNotNull(requiredFooQualifier);
        // FooQualifier#alpha() is @Nonbinding
        assertTrue(Beans.hasQualifier(bean, requiredFooQualifier));
    }

}
